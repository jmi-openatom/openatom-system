package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拒绝退社申请请求
 *
 * <p>用于管理员驳回成员的退社申请, 携带驳回意见comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestRejectExitApplicationDTO {
  private String comment;
}
