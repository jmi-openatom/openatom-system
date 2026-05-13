package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存校历基础设置请求 */
@Data
public class RequestSaveSchoolCalendarDTO {
  @NotBlank(message = "开学日期不能为空")
  private String startDate;

  @NotNull(message = "周数不能为空")
  @Min(value = 1, message = "周数至少为1")
  @Max(value = 60, message = "周数不能超过60")
  private Integer weekCount;
}
