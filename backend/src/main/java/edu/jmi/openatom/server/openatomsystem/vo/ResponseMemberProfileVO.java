package edu.jmi.openatom.server.openatomsystem.vo;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMemberProfileVO {
  private String slug;
  private String displayName;
  private String headline;
  private String bio;
  private String avatarUrl;
  private String bannerUrl;
  private String cardBackgroundUrl;
  private BigDecimal cardFocusX;
  private BigDecimal cardFocusY;
  private String departmentName;
  private String positionName;
  private List<String> skills;
  private String themeKey;
  private String colorMode;
  private String visibility;
  private String status;
  private Boolean showDepartment;
  private Boolean showPosition;
  private Integer version;
  private Boolean owner;
  private Boolean customized;
  private Long likeCount;
  private Boolean liked;
  private Long commentCount;
  private Timestamp publishedAt;
  private Timestamp updatedAt;
  private List<ModuleVO> modules;
  private List<SocialLinkVO> socialLinks;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ModuleVO {
    private Long id;
    private String key;
    private String title;
    private Integer sortOrder;
    private Integer columnSpan;
    private Boolean enabled;
    private String visibility;
    private Map<String, Object> data;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SocialLinkVO {
    private Long id;
    private String platform;
    private String label;
    private String url;
    private Integer sortOrder;
    private Boolean enabled;
  }
}
