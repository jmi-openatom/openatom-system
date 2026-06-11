package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 晚自习签到计划保存请求 */
@Data
public class RequestEveningStudyScheduleDTO {
  @NotNull(message = "请选择实验室分组")
  private Integer groupId;

  @NotBlank(message = "计划标题不能为空")
  private String title;

  private String location;

  @NotBlank(message = "开始时间不能为空")
  private String startTime;

  @NotBlank(message = "结束时间不能为空")
  private String endTime;

  private Long checkinPoints;
  private Integer checkinWindowMinutes;
  private Integer lateAfterMinutes;
  private Long latePenaltyPoints;
  private Long absentPenaltyPoints;
  private Boolean enabled;
}
