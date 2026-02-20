package com.gmavrommatis.config.client;

import com.gmavrommatis.model.response.PetClinicResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import jakarta.inject.Singleton;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Low-level client for Pet Clinic HTTP operations.
 *
 * <p>Offers both synchronous and asynchronous calls to /details, with detailed logging of errors
 * and thread execution context.
 *
 * <p>This component wraps a Micronaut {@link HttpClient} bound to the "petclinic" service.
 *
 * @author Georgios Mavrommatis
 */
@Singleton
@Slf4j
public class PetClinicLowLevelClient {

  private static final String USER_AGENT_HEADER = "Micronaut HTTP Client";
  private static final String ACCEPT_JSON = "application/json";

  private final HttpClient client;

  /**
   * Injects the named HTTP client for Pet Clinic service.
   *
   * @param client bound to the "petclinic" service in configuration
   */
  public PetClinicLowLevelClient(@Client(id = "petclinic") HttpClient client) {
    this.client = client;
  }

  /**
   * Retrieves clinic details in a blocking manner.
   *
   * <p>Constructs and fires a GET request, blocking until the response is received. HTTP error
   * statuses result in {@link HttpClientResponseException}, transport issues throw a {@link
   * RuntimeException}, both of which are logged.
   *
   * @param pageIndex zero-based page index to fetch
   * @param pageSize number of records per page
   * @return the deserialized {@link PetClinicResponse}
   * @throws HttpClientResponseException on HTTP 4xx/5xx status
   * @throws RuntimeException on network or timeout errors
   */
  public PetClinicResponse getClinicDetailsSync(int pageIndex, int pageSize) {
    HttpRequest<?> request = buildDetailsRequest(pageIndex, pageSize);
    try {
      // blocking call on I/O thread pool to avoid Netty event loop blocking
      return client.toBlocking().retrieve(request, Argument.of(PetClinicResponse.class));
    } catch (HttpClientResponseException httpEx) {
      log.error("HTTP {} for {}", httpEx.getStatus(), request.getUri(), httpEx);
      throw httpEx;
    } catch (RuntimeException transportEx) {
      /* catch-all for DNS, timeouts, etc. */
      log.error("Transport failure for {}", request.getUri(), transportEx);
      throw transportEx;
    }
  }

  /**
   * Retrieves clinic details asynchronously.
   *
   * <p>Returns a Reactive Streams {@link Publisher} emitting a single {@link PetClinicResponse}.
   * Errors are logged and propagated downstream.
   *
   * @param pageIndex zero-based page index to fetch
   * @param pageSize number of records per page
   * @return a {@link Publisher} emitting the response
   */
  public Publisher<PetClinicResponse> getClinicDetailsAsync(int pageIndex, int pageSize) {
    HttpRequest<?> request = buildDetailsRequest(pageIndex, pageSize);
    return Flux.from(client.retrieve(request, Argument.of(PetClinicResponse.class)))
        .doOnError(
            e -> {
              if (e instanceof HttpClientResponseException respEx) {
                log.error("HTTP {} for {}", respEx.getStatus(), request.getUri(), respEx);
              } else {
                // network/transport error during async request
                log.error("Async transport error for {}", request.getUri(), e);
              }
            });
  }

  /**
   * Builds a GET request for the /details endpoint with standard headers.
   *
   * @param pageIndex zero-based page index
   * @param pageSize number of records per page
   * @return configured {@link HttpRequest}
   */
  private HttpRequest<?> buildDetailsRequest(int pageIndex, int pageSize) {
    URI uri =
        UriBuilder.of("/details")
            .queryParam("page", pageIndex)
            .queryParam("size", pageSize)
            .build();
    // include default headers for tracing and content negotiation
    return HttpRequest.GET(uri)
        .header(HttpHeaders.USER_AGENT, USER_AGENT_HEADER)
        .header(HttpHeaders.ACCEPT, ACCEPT_JSON);
  }
}
