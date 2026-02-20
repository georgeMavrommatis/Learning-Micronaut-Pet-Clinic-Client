package com.gmavrommatis.service;

import com.gmavrommatis.config.client.PetClinicDeclarativeClient;
import com.gmavrommatis.config.client.PetClinicLowLevelClient;
import com.gmavrommatis.model.response.PetClinicResponse;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Service layer for Pet Clinic operations.
 *
 * @author GewrgiosMmavrommatis
 * @version 1.0
 */
@Singleton
@Slf4j
public class PetClinicService {

  private final PetClinicLowLevelClient lowLevelClient;
  private final PetClinicDeclarativeClient declarativeClient;

  public PetClinicService(
      PetClinicLowLevelClient lowLevelClient, PetClinicDeclarativeClient declarativeClient) {
    this.lowLevelClient = lowLevelClient;
    this.declarativeClient = declarativeClient;
  }

  public PetClinicResponse getPetClinicDetailsLowLevelSync(int page, int size) {
    return lowLevelClient.getClinicDetailsSync(page, size);
  }

  public Publisher<PetClinicResponse> getPetClinicDetailsLowLevelAsync(int page, int size) {
    return lowLevelClient.getClinicDetailsAsync(page, size);
  }

  public HttpResponse<PetClinicResponse> getPetClinicDetails(int page, int size) {
    return declarativeClient.retrieveClinicDetails(page, size);
  }

  public Mono<HttpResponse<PetClinicResponse>> getPetClinicDetailsAsync(int page, int size) {
    return Mono.from(declarativeClient.retrieveClinicDetailsAsync(page, size));
  }
}
