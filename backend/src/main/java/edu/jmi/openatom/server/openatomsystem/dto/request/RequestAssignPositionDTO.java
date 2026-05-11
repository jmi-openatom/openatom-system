package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配岗位请求
 *
 * <p>用于为用户分配指定岗位, 携带岗位ID positionId和生效时间effectiveAt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestAssignPositionDTO {
  @NotNull(message = "positionId不能为空")
  private Integer positionId;

  private String effectiveAt;
}
