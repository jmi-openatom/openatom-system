package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建成员关系请求
 *
 * <p>用于建立用户与社团的成员关系, 包含用户ID userId, 社团ID clubId, 所属部门ID departmentId, 岗位ID positionId, 成员状态status, 是否推荐featured和排序sortOrder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateMembershipDTO {
  @NotNull(message = "userId不能为空")
  private Integer userId;

  @NotNull(message = "clubId不能为空")
  private Integer clubId;

  private Integer departmentId;
  private Integer positionId;
  private String status;
  private Boolean featured;
  private Integer sortOrder;
  private String alumniGroup;
}
