package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseActivationVO {
  private Integer userId;
  private String userName;
  private String realName;
  private String avatar;
  private String qqOpenid;
  private Boolean activated;
  private Timestamp activatedAt;

  private MembershipInfo membership;
  private Leader departmentHead;
  private Leader viceDepartmentHead;
  private Leader president;
  private Leader leagueSecretary;
  private List<Leader> vicePresidents;
  private ClubInfo club;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MembershipInfo {
    private Integer clubId;
    private Integer departmentId;
    private String departmentName;
    private String departmentDescription;
    private String departmentWechatQrcode;
    private Integer positionId;
    private String positionName;
    private String status;
    private Timestamp joinedAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClubInfo {
    private Integer id;
    private String name;
    private String code;
    private String category;
    private String description;
    private String logoUrl;
    private String wechatGroupQrcode;
    private String qqGroupNumber;
    private List<FocusArea> focusAreas;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FocusArea {
    private String title;
    private String description;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Leader {
    private Integer userId;
    private String name;
    private String initial;
    private String role;
    private String avatar;
    private String qqAvatar;
  }
}
