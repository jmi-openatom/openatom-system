package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResponseSchoolCalendarVO {
  private Integer id;
  private String startDate;
  private Integer weekCount;
  private String endDate;
  private List<ResponseSchoolCalendarDayVO> days;
}
