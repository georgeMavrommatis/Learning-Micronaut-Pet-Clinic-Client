package com.gmavrommatis.controller;

import com.gmavrommatis.model.response.VetReviewResponse;
import com.gmavrommatis.service.VetReviewService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.sse.Event;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing various endpoints for streaming Vet Review details.
 *
 * <p>Supports batch JSON responses, SSE streams without headers, and SSE streams including metadata
 * headers in the initial handshake. Execution contexts are logged for troubleshooting. Base path:
 * <code>/vet-review</code>.
 *
 * @author Georgios Mavrommatis
 */
@Controller("/vet-review")
@Slf4j
public class VetReviewController {

  private static final int DEFAULT_OFFSET = 0;
  private static final int DEFAULT_LIMIT = 10;
  private static final String HEADER_LIMIT = "X-Limit";
  private static final String HEADER_OFFSET = "X-Offset";
  private static final String HEADER_TOTAL = "X-Total-Count";
  private static final String HEADER_EXPECTED = "X-Expected-Stream-Size";

  private final VetReviewService service;

  /**
   * Constructs controller with injected VetReviewService.
   *
   * @param service service handling business logic and HTTP calls
   */
  public VetReviewController(VetReviewService service) {
    this.service = service;
  }

  /**
   * Retrieves a JSON batch of reviews (non-streaming).
   *
   * <p>Serializes the entire Flux as a JSON array. Logs the thread context on subscribe.
   *
   * @param offset zero-based start index
   * @param limit max items to return
   * @return HTTP 200 with Flux<VetReviewResponse> body
   */
  @Get(uri = "/details", produces = MediaType.APPLICATION_JSON)
  public HttpResponse<Flux<VetReviewResponse>> getReviewBatch(
      @QueryValue(defaultValue = "0") int offset, @QueryValue(defaultValue = "10") int limit) {
    // subscribe and log execution context
    Flux<VetReviewResponse> flux =
        service.getVetReviewDetailsAsync(offset, limit).doOnSubscribe(sub -> logExecutionContext());

    return HttpResponse.ok(flux);
  }

  /**
   * Streams reviews as SSE without metadata headers.
   *
   * <p>Each VetReviewResponse is sent as an SSE event named 'message'. Logs thread on each
   * subscription.
   *
   * @param offset zero-based start index
   * @param limit max items per batch
   * @return Flux<Event<VetReviewResponse>> SSE stream
   */
  @Get(uri = "/details/stream/sse-no-headers", produces = MediaType.TEXT_EVENT_STREAM)
  public MutableHttpResponse<Flux<Event<VetReviewResponse>>> streamReviewsSse(
      @QueryValue(defaultValue = "0") int offset, @QueryValue(defaultValue = "10") int limit) {
    Flux<Event<VetReviewResponse>> stream =
        service
            .getVetReviewDetailsAsync(offset, limit)
            .doOnSubscribe(sub -> logExecutionContext())
            .map(Event::of);
    // Here we return the headers, but cannot decide about the stream before actually executing the
    // stream. Due to Declarative client nature.
    return HttpResponse.ok(stream)
        .header("X-Offset", String.valueOf(offset))
        .header("X-Limit", String.valueOf(limit))
        .header("X-Stream-Type", "vet-reviews");
  }

  /**
   * Streams reviews as SSE including metadata headers in the handshake.
   *
   * <p>Initial HTTP response carries custom headers: Limit, Offset, Total-Count,
   * Expected-Stream-Size. Subsequent SSE body streams Event-wrapped payloads.
   *
   * @param offset zero-based start index
   * @param limit max items per batch
   * @return Mono<HttpResponse<Flux<Event<VetReviewResponse>>>> wrapped SSE stream
   */
  @Get(uri = "/details/stream/sse-include-headers", produces = MediaType.TEXT_EVENT_STREAM)
  public Mono<HttpResponse<Flux<Event<VetReviewResponse>>>> streamReviewsSseWithHeaders(
      @QueryValue(defaultValue = "0") int offset, @QueryValue(defaultValue = "10") int limit) {
    // low-level service provides headers + Flux body
    return service
        .getVetReviewDetailsAsyncLowLevel(offset, limit)
        .map(
            resp -> {
              Flux<Event<VetReviewResponse>> sseFlux =
                  resp.body().doOnSubscribe(sub -> logExecutionContext()).map(Event::of);

              // build response with preserved headers
              return HttpResponse.ok(sseFlux)
                  .contentType(MediaType.TEXT_EVENT_STREAM)
                  .headers(
                      h -> {
                        // copy each custom header
                        h.add(HEADER_LIMIT, resp.getHeaders().get("Limit"));
                        h.add(HEADER_OFFSET, resp.getHeaders().get("Offset"));
                        h.add(HEADER_TOTAL, resp.getHeaders().get("Total-Count"));
                        h.add(HEADER_EXPECTED, resp.getHeaders().get("Expected-Stream-Size"));
                      });
            });
  }

  /** Logs whether current thread is Event Loop or Worker. */
  private void logExecutionContext() {
    String thread = Thread.currentThread().getName();
    String pool = thread.contains("nioEventLoopGroup") ? "EVENT_LOOP" : "WORKER";
    log.info("→ executed on {} thread: {}", pool, thread);
  }
}
