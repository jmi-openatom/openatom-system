package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RequestRescheduleInterviewSessionDTO {
  /** 留空时沿用场次当前设置。 */
  private String scheduledStartAt;
  @Min(value = 5, message = "单场时长至少5分钟") private Integer durationMinutes;
  @Min(value = 0, message = "间隔不能为负数") private Integer gapMinutes;
  private Boolean previewOnly = true;
  /** 预览对应的场次版本，防止确认时覆盖期间新增的人员或面试间配置。 */
  private String expectedRevision;
}
