package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAiSettingsDTO {
  private String baseUrl;
  private String apiKey;
  private String model;
  private Integer timeoutSeconds;
  private Boolean enabled;
}
