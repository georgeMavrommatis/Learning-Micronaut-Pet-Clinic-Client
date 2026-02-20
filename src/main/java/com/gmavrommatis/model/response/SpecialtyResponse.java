package com.gmavrommatis.model.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Introspected
@Serdeable
@Data
@Builder
public class SpecialtyResponse {
  private String name;
}
