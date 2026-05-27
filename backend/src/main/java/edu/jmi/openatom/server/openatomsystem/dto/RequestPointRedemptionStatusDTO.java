package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestPointRedemptionStatusDTO {
  @NotBlank(message = "兑换状态不能为空")
  private String status;

  private String adminNote;
}
