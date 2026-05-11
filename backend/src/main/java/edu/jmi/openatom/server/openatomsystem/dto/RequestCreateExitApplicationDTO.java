package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建退社申请请求
 *
 * <p>用于成员提交退社申请, 包含成员关系ID membershipId, 退社原因reason和补充说明description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateExitApplicationDTO {
  @NotNull(message = "membershipId不能为空")
  private Integer membershipId;

  @NotBlank(message = "退社原因不能为空")
  private String reason;

  private String description;
}
