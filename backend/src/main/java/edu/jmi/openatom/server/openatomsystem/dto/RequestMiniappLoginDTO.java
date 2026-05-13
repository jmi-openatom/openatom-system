package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestMiniappLoginDTO {
  @NotBlank(message = "小程序登录code不能为空")
  private String code;
}
