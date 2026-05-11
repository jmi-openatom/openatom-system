package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新成员关系请求
 *
 * <p>用于更新成员关系信息, 包含所属部门ID departmentId, 岗位ID positionId, 成员状态status, 是否推荐featured和排序sortOrder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateMembershipDTO {
  private Integer departmentId;
  private Integer positionId;
  private String status;
  private Boolean featured;
  private Integer sortOrder;
}
