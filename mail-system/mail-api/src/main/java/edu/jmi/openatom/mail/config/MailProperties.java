package edu.jmi.openatom.mail.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {
  private String domain = "jmi-openatom.cn";
  private String addressSalt;
  private long defaultQuotaBytes = 2_147_483_648L;
  private String internalServiceToken;
  private String mainSiteUsersUrl;
  private String broadcastFrom = "official@mailer.jmi-openatom.cn";
  private int broadcastMaxRecipients = 200;
  private Stalwart stalwart = new Stalwart();
  private OAuth oauth = new OAuth();
  private MalwareScanner malwareScanner = new MalwareScanner();

  @PostConstruct
  void validate() {
    if (domain == null || !domain.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}$")) {
      throw new IllegalStateException("mail.domain is invalid");
    }
    requireSecret(addressSalt, "MAIL_ADDRESS_SALT");
    requireSecret(internalServiceToken, "MAIL_INTERNAL_SERVICE_TOKEN");
    requireSecret(stalwart.apiToken, "STALWART_API_TOKEN");
    // STALWART_DOMAIN_ID is a Stalwart object id (a short base32 string such
    // as "b"), not a secret; only require it to be present.
    if (stalwart.domainId == null || stalwart.domainId.isBlank()) {
      throw new IllegalStateException("STALWART_DOMAIN_ID is required");
    }
    URI.create(stalwart.apiUrl);
    URI.create(stalwart.sessionUrl);
    URI.create(stalwart.jmapUrl);
    if (mainSiteUsersUrl != null && !mainSiteUsersUrl.isBlank()) {
      URI.create(mainSiteUsersUrl);
    }
    requireSecret(oauth.clientSecret, "MAIL_OAUTH_CLIENT_SECRET");
    URI.create(oauth.issuer);
    URI.create(oauth.authorizationUrl);
    URI.create(oauth.tokenUrl);
    URI.create(oauth.jwksUrl);
    URI.create(oauth.redirectUri);
    if (malwareScanner.host == null || malwareScanner.host.isBlank()) {
      throw new IllegalStateException("MAIL_CLAMAV_HOST is required");
    }
    if (malwareScanner.port < 1 || malwareScanner.port > 65535) {
      throw new IllegalStateException("MAIL_CLAMAV_PORT is invalid");
    }
  }

  private void requireSecret(String value, String name) {
    if (value == null || value.length() < 16) {
      throw new IllegalStateException(name + " must contain at least 16 characters");
    }
  }

  public String getDomain() { return domain; }
  public void setDomain(String domain) { this.domain = domain; }
  public String getAddressSalt() { return addressSalt; }
  public void setAddressSalt(String addressSalt) { this.addressSalt = addressSalt; }
  public long getDefaultQuotaBytes() { return defaultQuotaBytes; }
  public void setDefaultQuotaBytes(long defaultQuotaBytes) { this.defaultQuotaBytes = defaultQuotaBytes; }
  public String getInternalServiceToken() { return internalServiceToken; }
  public void setInternalServiceToken(String internalServiceToken) { this.internalServiceToken = internalServiceToken; }
  public String getMainSiteUsersUrl() { return mainSiteUsersUrl; }
  public void setMainSiteUsersUrl(String mainSiteUsersUrl) { this.mainSiteUsersUrl = mainSiteUsersUrl; }
  public String getBroadcastFrom() { return broadcastFrom; }
  public void setBroadcastFrom(String broadcastFrom) { this.broadcastFrom = broadcastFrom; }
  public int getBroadcastMaxRecipients() { return broadcastMaxRecipients; }
  public void setBroadcastMaxRecipients(int broadcastMaxRecipients) { this.broadcastMaxRecipients = broadcastMaxRecipients; }
  public Stalwart getStalwart() { return stalwart; }
  public void setStalwart(Stalwart stalwart) { this.stalwart = stalwart; }
  public OAuth getOauth() { return oauth; }
  public void setOauth(OAuth oauth) { this.oauth = oauth; }
  public MalwareScanner getMalwareScanner() { return malwareScanner; }
  public void setMalwareScanner(MalwareScanner malwareScanner) { this.malwareScanner = malwareScanner; }

  public static class Stalwart {
    private String apiUrl;
    private String sessionUrl;
    private String jmapUrl;
    private String apiToken;
    private String domainId;
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getSessionUrl() { return sessionUrl; }
    public void setSessionUrl(String sessionUrl) { this.sessionUrl = sessionUrl; }
    public String getJmapUrl() { return jmapUrl; }
    public void setJmapUrl(String jmapUrl) { this.jmapUrl = jmapUrl; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
  }

  public static class OAuth {
    private String issuer;
    private String authorizationUrl;
    private String tokenUrl;
    private String jwksUrl;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getAuthorizationUrl() { return authorizationUrl; }
    public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }
    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
    public String getJwksUrl() { return jwksUrl; }
    public void setJwksUrl(String jwksUrl) { this.jwksUrl = jwksUrl; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
  }

  public static class MalwareScanner {
    private String host = "clamav";
    private int port = 3310;
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 30000;
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
  }
}
