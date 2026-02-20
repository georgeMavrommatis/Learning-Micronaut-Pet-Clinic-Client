package com.gmavrommatis.config.client;

import com.gmavrommatis.model.response.VetReviewResponse;
import io.micronaut.core.io.buffer.ByteBuffer;
import io.micronaut.core.type.Argument;
import io.micronaut.http.*;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.reactor.http.client.ReactorStreamingHttpClient;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Low-level reactive client for streaming reviewer details from Vet Review service.
 *
 * <p>Streams reviewer details as a JSON stream, supports backpressure, and propagates HTTP headers
 * alongside the payload. Logs each streamed item and errors.
 *
 * <p>This component wraps both a standard {@link HttpClient} and a {@link
 * ReactorStreamingHttpClient} bound to the "vetreview" service.
 *
 * @author Georgios
 */
@Singleton
@Slf4j
public class VetReviewLowLevelClient {

  private static final String DEFAULT_URL = "/reviewer-details/json-stream/back-pressure/batch";
  private static final String USER_AGENT_VALUE = "Micronaut HTTP Client";
  private static final String JSON_STREAM = MediaType.APPLICATION_JSON_STREAM;

  private final ReactorStreamingHttpClient streamingClient;
  private final ObjectMapper mapper;

  public VetReviewLowLevelClient(
      @Client(id = "vetreview") ReactorStreamingHttpClient streamingClient, ObjectMapper mapper) {
    this.streamingClient = streamingClient;
    this.mapper = mapper;
  }

  /**
   * Streams a batch of reviewer details with a single HTTP request.
   *
   * <p>1. Issues one GET to the /reviewer-details/json-stream/back-pressure/batch endpoint. 2.
   * Caches the first response frame to extract HTTP headers exactly once. 3. Parses every JSON
   * payload chunk into {@link VetReviewResponse}. 4. Returns a {@code
   * Mono<HttpResponse<Flux<VetReviewResponse>>>} allowing header inspection followed by a reactive
   * stream of DTOs.
   *
   * @param offset zero-based starting index for pagination
   * @param limit maximum number of items to retrieve in this batch
   * @return a Mono emitting an HttpResponse whose body is a Flux of VetReviewResponse
   * @throws RuntimeException if JSON parsing of any chunk fails
   */
  public Mono<HttpResponse<Flux<VetReviewResponse>>> streamReviewBatch(int offset, int limit) {

    HttpRequest<?> request = buildBatchRequest(offset, limit);

    // Execute the streaming request ONCE and cache the first frame
    Flux<HttpResponse<ByteBuffer<?>>> shared =
        Flux.from(streamingClient.exchangeStream(request, Argument.of(VetReviewResponse.class)))
            .cache(1); // cache first element (headers + status)

    // Extract and validate FIRST response (control plane)
    Mono<HttpResponse<ByteBuffer<?>>> firstResponseMono = shared.next();

    Mono<HttpHeaders> validatedHeaders =
        firstResponseMono.flatMap(
            resp -> {
              HttpStatus status = resp.getStatus();

              // ---- STATUS CHECK ----
              if (status.getCode() >= 400) {
                return Mono.error(
                    new IllegalStateException(
                        "Upstream error: " + status + ", headers=" + resp.getHeaders()));
              }

              // ---- HEADER INSPECTION ----
              String expectedStreamSize = resp.getHeaders().get("Expected-Stream-Size");
              if (Integer.parseInt(expectedStreamSize) < limit) {
                log.warn(
                    "We are reaching the end of available reviews, and the current response total is expected less that usual");
              }

              // ---- CONTENT TYPE CHECK ----
              MediaType ct = resp.getContentType().orElse(null);
              if (ct == null || !ct.getName().equals(JSON_STREAM)) {
                return Mono.error(new IllegalStateException("Unexpected Content-Type: " + ct));
              }

              return Mono.just(resp.getHeaders());
            });

    // 3 Decode BODY ONLY AFTER header validation
    Flux<VetReviewResponse> bodyFlux =
        shared
            .skip(1) // skip cached header frame
            .flatMap(
                frame ->
                    frame
                        .getBody()
                        .map(
                            buffer -> {
                              try {
                                return mapper.readValue(
                                    buffer.toByteArray(), VetReviewResponse.class);
                              } catch (IOException e) {
                                throw new RuntimeException("Failed to parse JSON chunk", e);
                              }
                            })
                        .map(Mono::just)
                        .orElseGet(Mono::empty))
            .doOnNext(vr -> log.info("Received review {}", vr))
            .doOnError(err -> log.error("Streaming error", err));

    // 4️ Recombine headers + streaming body
    return validatedHeaders.map(
        hdrs ->
            HttpResponse.ok(bodyFlux)
                .contentType(JSON_STREAM)
                .headers(h -> hdrs.forEach((k, vals) -> vals.forEach(v -> h.add(k, v)))));
  }

  /**
   * Constructs a GET request for the batch streaming endpoint.
   *
   * @param offset zero-based starting position
   * @param limit max number of items
   * @return configured HttpRequest
   */
  private HttpRequest<?> buildBatchRequest(int offset, int limit) {
    URI uri =
        UriBuilder.of(DEFAULT_URL).queryParam("offset", offset).queryParam("limit", limit).build();
    return HttpRequest.GET(uri)
        .header(HttpHeaders.USER_AGENT, USER_AGENT_VALUE)
        .accept(JSON_STREAM);
  }
}
