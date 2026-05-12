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
public class ResponseCheckInRecordVO {
  private Integer userId;
  private String userName;
  private String realName;
  private String studentId;
  private String phone;
  private String status;
  private Timestamp checkinAt;
}
