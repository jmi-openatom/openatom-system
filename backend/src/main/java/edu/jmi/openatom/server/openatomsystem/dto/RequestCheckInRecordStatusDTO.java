package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestCheckInRecordStatusDTO {
  @NotBlank(message = "签到状态不能为空")
  private String status;
}
