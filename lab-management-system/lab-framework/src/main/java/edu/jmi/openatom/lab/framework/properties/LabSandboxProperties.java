package edu.jmi.openatom.lab.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "lab.sandbox")
public class LabSandboxProperties {
  private String endpoint;
  private int timeoutMs = 5000;
}
