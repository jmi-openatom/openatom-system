package edu.jmi.openatom.lab.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_checkin_session")
public class LabCheckInSession {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long groupId;
  private Long scheduleId;
  private String sessionType;
  private LocalDate attendanceDate;
  private String title;
  private String location;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String status;
  private String token;
  private Integer checkinPoints;
  private Integer checkinWindowMinutes;
  private Integer lateAfterMinutes;
  private Integer latePenaltyPoints;
  private Integer absentPenaltyPoints;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
