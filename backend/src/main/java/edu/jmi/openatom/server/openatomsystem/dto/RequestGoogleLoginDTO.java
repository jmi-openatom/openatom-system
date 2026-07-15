package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestGoogleLoginDTO {
  @NotBlank(message = "Google credential不能为空")
  private String credential;
}
