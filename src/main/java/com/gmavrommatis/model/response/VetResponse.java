package com.gmavrommatis.model.response;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.Data;

@Introspected
@Serdeable
@Data
public class VetResponse {
  private String firstName;
  private String lastName;
  private List<SpecialtyResponse> specialties;
}
