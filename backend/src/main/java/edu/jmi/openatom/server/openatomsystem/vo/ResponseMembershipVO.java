package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员信息VO
 *
 * <p>包含成员ID, 所属社团与部门信息, 职位详情, 成员状态, 是否核心成员及排序和加入时间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMembershipVO {
  private Integer id;
  private Integer userId;
  private String userName;
  private String realName;
  private Integer clubId;
  private String clubName;
  private Integer departmentId;
  private String departmentName;
  private Integer positionId;
  private String positionName;
  private String status;
  private Boolean featured;
  private Integer sortOrder;
  private Timestamp joinedAt;
  private Timestamp leftAt;
  private String alumniGroup;
}
