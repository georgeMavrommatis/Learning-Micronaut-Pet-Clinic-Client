package com.gmavrommatis.config.client;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

import com.gmavrommatis.model.response.PetClinicResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import reactor.core.publisher.Mono;

/**
 * Client for Pet Clinic operations.
 *
 * <p>Offers both blocking and non-blocking access to the /details endpoint.
 *
 * @author Georgios Mavrommatis
 */
@Client(id = "petclinic")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client") // identify this client in requests
@Header(name = ACCEPT, value = "application/json") /* we expect JSON responses */
public interface PetClinicDeclarativeClient {

  @Get("/details{?page,size}")
  HttpResponse<PetClinicResponse> retrieveClinicDetails(
      @QueryValue(value = "page", defaultValue = "0") int page,
      @QueryValue(value = "size", defaultValue = "10") int size);

  /*
   * Retrieves clinic details reactively.
   *
   * Returns a Publisher that will emit a single PetClinicResponse item.
   * This supports backpressure, although only one element is expected.
   */
  @Get("/details{?page,size}")
  Mono<HttpResponse<PetClinicResponse>> retrieveClinicDetailsAsync(
      @QueryValue(value = "page", defaultValue = "0") int page,
      @QueryValue(value = "size", defaultValue = "10") int size);
}
