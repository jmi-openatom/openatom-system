package edu.jmi.openatom.server.openatomsystem.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RequestSaveMemberProfileDTO {
  private String slug;
  private String displayName;
  private String headline;
  private String bio;
  private String avatarUrl;
  private String bannerUrl;
  private String cardBackgroundUrl;
  private BigDecimal cardFocusX;
  private BigDecimal cardFocusY;
  private String themeKey;
  private String colorMode;
  private String visibility;
  private Boolean commentsEnabled;
  private Boolean showDepartment;
  private Boolean showPosition;
  private List<String> skills;
  private Integer version;
  private List<ModuleItem> modules;
  private List<SocialLinkItem> socialLinks;

  @Data
  public static class ModuleItem {
    private String key;
    private String title;
    private Integer sortOrder;
    private Integer columnSpan;
    private Boolean enabled;
    private String visibility;
    private Map<String, Object> config;
  }

  @Data
  public static class SocialLinkItem {
    private String platform;
    private String label;
    private String url;
    private Integer sortOrder;
    private Boolean enabled;
  }
}
