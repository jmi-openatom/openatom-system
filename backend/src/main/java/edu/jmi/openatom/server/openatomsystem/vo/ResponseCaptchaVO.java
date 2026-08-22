package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 滑块拼图验证码响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseCaptchaVO {
  private String captchaId;
  private String backgroundBase64;
  private String pieceBase64;
  private int pieceY;
}