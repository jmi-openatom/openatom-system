package edu.jmi.openatom.server.openatomsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.seafile-oauth")
public class SeafileOauthClientProperties {
  private String clientId = "openatom-seafile";
  private String clientSecret;
  private String clientName = "OpenAtom Seafile";
  private String redirectUri = "https://cloud.jmi-openatom.cn/oauth/callback/";
}
