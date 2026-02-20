package com.gmavrommatis.controller;

import com.gmavrommatis.model.response.PetClinicResponse;
import com.gmavrommatis.service.PetClinicService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing multiple styles for retrieving Pet Clinic details.
 *
 * <p>Supports low-level blocking, low-level reactive streams, standard service-layer blocking
 * calls, and reactive Mono endpoints. Logs the execution context for each call.
 *
 * <p>Base path: <code>/pet-client</code>.
 *
 * @author gewrgios mavrommatis
 */
@Controller("/pet-client")
@Slf4j
public class PetClinicController {

  private static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON;

  private final PetClinicService service;

  /**
   * Injects the PetClinicService which encapsulates business logic and HTTP interactions.
   *
   * @param service application service for retrieving Pet Clinic data
   */
  public PetClinicController(PetClinicService service) {
    this.service = service;
  }

  /**
   * Blocking endpoint using low-level HTTP client.
   *
   * <p>Runs on a worker I/O thread, not the Netty event loop. Logs which pool executed the request.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @return HTTP 200 with PetClinicResponse payload
   * @throws Exception for unexpected errors
   */
  @Get(uri = "/pet-clinic-details-low-level-sync", produces = CONTENT_TYPE_JSON)
  public HttpResponse<PetClinicResponse> getLowLevelSync(
      @QueryValue(defaultValue = "0") int page, @QueryValue(defaultValue = "10") int size) {
    logExecutionContext();
    PetClinicResponse response = service.getPetClinicDetailsLowLevelSync(page, size);
    return HttpResponse.ok(response);
  }

  /**
   * Reactive endpoint using low-level streaming client.
   *
   * <p>Emits each response as a stream; logs the thread context for each element.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @return Publisher of HTTP responses carrying PetClinicResponse
   */
  @Get(uri = "/pet-clinic-details-low-level-async", produces = CONTENT_TYPE_JSON)
  public Publisher<HttpResponse<PetClinicResponse>> getLowLevelAsync(
      @QueryValue(defaultValue = "0") int page, @QueryValue(defaultValue = "10") int size) {
    return Flux.from(service.getPetClinicDetailsLowLevelAsync(page, size))
        .doOnNext(resp -> logExecutionContext())
        .map(
            resp ->
                HttpResponse.ok(resp)
                    .header("X-Page", String.valueOf(page))
                    .header("X-Size", String.valueOf(size))
                    .header("X-Result-Count", String.valueOf(resp.getVets().size())));
  }

  /**
   * Standard blocking endpoint using service-layer abstraction.
   *
   * <p>Logs execution context and preserves response headers/status from service call.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @return HttpResponse wrapping the service's response
   * @throws Exception for service errors
   */
  @Get(uri = "/pet-clinic-details", produces = CONTENT_TYPE_JSON)
  public HttpResponse<PetClinicResponse> getStandard(
      @QueryValue(defaultValue = "0") int page, @QueryValue(defaultValue = "10") int size) {
    logExecutionContext();
    HttpResponse<PetClinicResponse> svcResponse = service.getPetClinicDetails(page, size);
    // rebuild response to retain headers and status
    return HttpResponse.status(svcResponse.getStatus())
        .headers(h -> svcResponse.getHeaders().forEach((k, vals) -> vals.forEach(v -> h.add(k, v))))
        .body(svcResponse.body());
  }

  /**
   * Reactive endpoint using service-layer Mono.
   *
   * <p>Returns a Mono that emits a single HTTP response and logs context when emitted.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @return Mono emitting HttpResponse of PetClinicResponse
   */
  @Get(uri = "/pet-clinic-details-async", produces = CONTENT_TYPE_JSON)
  public Mono<HttpResponse<PetClinicResponse>> getReactive(
      @QueryValue(defaultValue = "0") int page, @QueryValue(defaultValue = "10") int size) {
    Mono<HttpResponse<PetClinicResponse>> svcResponse =
        service.getPetClinicDetailsAsync(page, size).doOnNext(resp -> logExecutionContext());

    // rebuild response to retain headers and status
    return svcResponse.map(
        response ->
            HttpResponse.ok(response.body())
                .headers(
                    h ->
                        response
                            .getHeaders()
                            .forEach((key, values) -> values.forEach(v -> h.add(key, v)))));
  }

  /** Logs whether current thread belongs to Netty event-loop or worker pool. */
  private void logExecutionContext() {
    String thread = Thread.currentThread().getName();
    String poolType = thread.contains("nioEventLoopGroup") ? "EVENT_LOOP" : "WORKER";
    log.info("→ executed on {} thread: {}", poolType, thread);
  }
}
