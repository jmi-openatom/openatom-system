package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseEveningStudyScheduleVO {
  private Integer id;
  private Integer groupId;
  private String groupName;
  private Integer memberCount;
  private String title;
  private String location;
  private String startTime;
  private String endTime;
  private Long checkinPoints;
  private Boolean enabled;
  private Integer todaySessionId;
  private Integer todayTargetCount;
  private Integer todayCheckedCount;
  private Integer todayExcusedCount;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
