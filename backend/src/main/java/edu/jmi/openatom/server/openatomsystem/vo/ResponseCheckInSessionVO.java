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
  private String title;
  private String activityTitle;
  private String location;
  private Timestamp startAt;
  private Timestamp endAt;
  private String status;
  private String token;
  private String qrPayload;
  private Integer targetCount;
  private Integer checkedCount;
  private List<Integer> targetUserIds;
}
