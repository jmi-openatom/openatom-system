package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新令牌请求
 *
 * <p>用于刷新访问令牌, 携带refreshToken获取新的访问凭证
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestRefreshTokenDTO {
  @NotBlank(message = "refreshToken不能为空")
  private String refreshToken;
}
