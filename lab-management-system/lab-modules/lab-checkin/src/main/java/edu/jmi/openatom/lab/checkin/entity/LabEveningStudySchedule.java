package edu.jmi.openatom.lab.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

@Data
@TableName("lab_evening_study_schedule")
public class LabEveningStudySchedule {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long groupId;
  private String title;
  private String location;
  private LocalTime startTime;
  private LocalTime endTime;
  private Integer checkinPoints;
  private Integer checkinWindowMinutes;
  private Integer lateAfterMinutes;
  private Integer latePenaltyPoints;
  private Integer absentPenaltyPoints;
  private Boolean enabled;
  private Long createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
