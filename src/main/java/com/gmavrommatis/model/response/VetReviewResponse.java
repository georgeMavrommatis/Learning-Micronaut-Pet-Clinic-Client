package com.gmavrommatis.model.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

@Introspected
@Serdeable
@Getter
@Setter
@Builder
@ToString
public class VetReviewResponse {

  private String reviewer;

  private String content;

  private short rating;
}
