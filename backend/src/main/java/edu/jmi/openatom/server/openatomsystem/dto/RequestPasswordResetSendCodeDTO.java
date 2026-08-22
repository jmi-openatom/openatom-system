package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送忘记密码验证码请求
 *
 * <p>携带账号（用户名/学号/邮箱）和滑块拼图验证码，校验通过后向绑定邮箱发送忘记密码验证码邮件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPasswordResetSendCodeDTO {
  @NotBlank(message = "账号不能为空")
  @Size(max = 128, message = "账号长度不能超过128个字符")
  private String account;

  @NotBlank(message = "验证码ID不能为空")
  @Size(max = 64, message = "验证码ID格式不正确")
  private String captchaId;

  /** 滑块拼图缺口 X 坐标（背景图原始像素） */
  @NotNull(message = "滑块位置不能为空")
  private Integer captchaValue;
}