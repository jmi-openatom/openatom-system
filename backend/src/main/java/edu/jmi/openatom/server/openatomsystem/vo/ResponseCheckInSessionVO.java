package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseCheckInSessionVO {
  private Integer id;
  private Integer activityId;
  private Integer groupId;
  private Integer scheduleId;
  private String sessionType;
  private String attendanceDate;
  private String title;
  private String groupName;
  private String activityTitle;
  private String location;
  private Timestamp startAt;
  private Timestamp endAt;
  private String status;
  private String qrPayload;
  private Long checkinPoints;
  private Integer targetCount;
  private Integer checkedCount;
  private Integer pendingCount;
  private Integer excusedCount;
  private List<Integer> targetUserIds;
  private List<Integer> excusedUserIds;
}
