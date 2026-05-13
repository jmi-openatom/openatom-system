package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseSchoolCalendarDayVO {
  private String date;
  private Integer weekIndex;
  private Integer dayOfWeek;
  private String dayName;
  private Boolean restDay;
  private String source;
  private String adjustmentType;
  private String reason;
}
