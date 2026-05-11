package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 强制退社请求
 *
 * <p>用于管理员强制移除社团成员, 携带退社原因reason
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestForceExitDTO {
  private String reason;
}
