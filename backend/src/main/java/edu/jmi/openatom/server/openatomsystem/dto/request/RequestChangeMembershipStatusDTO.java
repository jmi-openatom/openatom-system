package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员状态变更请求
 *
 * <p>用于修改成员关系状态, 携带目标状态status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestChangeMembershipStatusDTO {
  @NotBlank(message = "成员状态不能为空")
  private String status;
}
