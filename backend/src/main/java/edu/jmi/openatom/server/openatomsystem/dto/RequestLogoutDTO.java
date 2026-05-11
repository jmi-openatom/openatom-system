package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登出请求
 *
 * <p>用于用户登出系统, 携带refreshToken用于清理令牌
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestLogoutDTO {
  private String refreshToken;
}
