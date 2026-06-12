package edu.jmi.openatom.lab.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "lab.ai")
public class LabAiProperties {
  private boolean enabled;
  private String endpoint;
  private String apiKey;
  private String model = "gpt-4.1-mini";
}
