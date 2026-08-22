package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPasswordResetDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestPasswordResetSendCodeDTO;

/**
 * 忘记密码服务
 *
 * <p>通过邮箱验证码完成密码重置：先按账号发送验证码邮件，再校验验证码并更新密码
 */
public interface PasswordResetService {

  /**
   * 向账号绑定的邮箱发送忘记密码验证码
   *
   * <p>无论账号是否存在都返回成功，避免泄露账号注册状态（防枚举）
   */
  Result<String> sendCode(RequestPasswordResetSendCodeDTO request);

  /** 校验验证码并重置密码，成功后作废该用户所有会话 */
  Result<String> reset(RequestPasswordResetDTO request);
}