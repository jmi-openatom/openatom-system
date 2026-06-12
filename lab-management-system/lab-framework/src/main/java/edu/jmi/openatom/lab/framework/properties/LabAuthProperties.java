package edu.jmi.openatom.lab.framework.properties;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "lab.auth")
public class LabAuthProperties {
  private String jwtSecret;
  private long tokenTtlMinutes = 720;
  private boolean devLoginEnabled = false;
  private OAuth oauth = new OAuth();

  @Data
  public static class OAuth {
    private String authorizeUrl;
    private String tokenUrl;
    private String userInfoUrl;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes = "openid profile";

    public List<String> scopeList() {
      if (scopes == null || scopes.isBlank()) {
        return List.of("openid", "profile");
      }
      return List.of(scopes.trim().split("\\s+"));
    }
  }
}
