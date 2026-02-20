package com.gmavrommatis.config.client;

import com.gmavrommatis.model.response.VetReviewResponse;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import org.reactivestreams.Publisher;

/**
 * Service client for streaming reviewer details from Vet Review.
 *
 * <p>Emits reviewer details as a JSON stream with built-in backpressure, allowing clients to
 * control flow via offset and batch size parameters.
 *
 * <p>This component is bound to the "vetreview" HTTP service by Micronaut's @Client. Author:
 * Georgios Mavrommatis
 */
@Client(id = "vetreview")
@Header(name = HttpHeaders.USER_AGENT, value = "Micronaut HTTP Client") // identify client
@Header(
    name = HttpHeaders.ACCEPT,
    value = MediaType.APPLICATION_JSON_STREAM) /* expect JSON Stream */
public interface VetReviewDeclarativeClient {

  /**
   * Streams reviewer detail records in batches.
   *
   * <p>Returns a reactive {@link Publisher} that emits {@link VetReviewResponse} items as they
   * arrive. Backpressure support allows the client to slow or speed up consumption based on
   * processing capacity.
   *
   * @param startPosition zero-based offset into the reviewer records
   * @param batchSize maximum number of records to retrieve in this invocation
   * @return a Publisher emitting reviewer detail objects
   */
  @Get("/reviewer-details/json-stream/back-pressure/batch{?offset,limit}")
  Publisher<VetReviewResponse> streamReviewerDetails(
      @QueryValue(value = "offset", defaultValue = "0") int startPosition,
      @QueryValue(value = "limit", defaultValue = "10") int batchSize);
}
