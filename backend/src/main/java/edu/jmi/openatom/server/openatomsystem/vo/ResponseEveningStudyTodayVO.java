package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseEveningStudyTodayVO {
  private String date;
  private Integer sessionCount;
  private Integer targetCount;
  private Integer checkedCount;
  private Integer pendingCount;
  private Integer excusedCount;
  private List<ResponseCheckInSessionVO> sessions;
}
