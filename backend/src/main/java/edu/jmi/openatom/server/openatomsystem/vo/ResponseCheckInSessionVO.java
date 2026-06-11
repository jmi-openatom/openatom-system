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
  private Timestamp checkinDeadlineAt;
  private String status;
  private String qrPayload;
  private Long checkinPoints;
  private Integer checkinWindowMinutes;
  private Integer lateAfterMinutes;
  private Long latePenaltyPoints;
  private Long absentPenaltyPoints;
  private Integer targetCount;
  private Integer signedCount;
  private Integer checkedCount;
  private Integer lateCount;
  private Integer absentCount;
  private Integer pendingCount;
  private Integer excusedCount;
  private List<Integer> targetUserIds;
  private List<Integer> excusedUserIds;
}
