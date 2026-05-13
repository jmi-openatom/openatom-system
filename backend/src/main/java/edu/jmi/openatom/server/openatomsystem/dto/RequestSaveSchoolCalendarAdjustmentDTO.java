package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 保存校历调休请求 */
@Data
public class RequestSaveSchoolCalendarAdjustmentDTO {
  @NotBlank(message = "日期不能为空")
  private String date;

  @NotBlank(message = "调休类型不能为空")
  private String type;

  private String reason;
}
