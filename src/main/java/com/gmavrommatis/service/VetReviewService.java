package com.gmavrommatis.service;

import com.gmavrommatis.config.client.VetReviewDeclarativeClient;
import com.gmavrommatis.config.client.VetReviewLowLevelClient;
import com.gmavrommatis.model.response.VetReviewResponse;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service layer for Pet Clinic operations.
 *
 * @author GewrgiosMmavrommatis
 * @version 1.0
 */
@Singleton
@Slf4j
public class VetReviewService {

  private final VetReviewLowLevelClient vetReviewLowLevelClient;
  private final VetReviewDeclarativeClient declarativeClient;

  public VetReviewService(
      VetReviewLowLevelClient vetReviewLowLevelClient,
      VetReviewDeclarativeClient declarativeClient) {
    this.vetReviewLowLevelClient = vetReviewLowLevelClient;
    this.declarativeClient = declarativeClient;
  }

  public Flux<VetReviewResponse> getVetReviewDetailsAsync(int offset, int limit) {

    return Flux.from(declarativeClient.streamReviewerDetails(offset, limit))
        // Streaming allows the client to utilize its resources and execute operations in parallel
        // as events are received
        .doOnNext(vetReviewResponse -> log.info("Working on review: {}", vetReviewResponse));
  }

  public Mono<HttpResponse<Flux<VetReviewResponse>>> getVetReviewDetailsAsyncLowLevel(
      int offset, int limit) {

    return Mono.from(vetReviewLowLevelClient.streamReviewBatch(offset, limit))
        // Streaming allows the client to utilize its resources and execute operations in parallel
        // as events are received
        .doOnNext(vetReviewResponse -> log.info("Working on review: {}", vetReviewResponse));
  }
}
