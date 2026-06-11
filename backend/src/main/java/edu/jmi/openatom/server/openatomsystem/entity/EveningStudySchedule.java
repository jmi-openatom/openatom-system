package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Time;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 晚自习每日签到计划 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("evening_study_schedule")
public class EveningStudySchedule {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("group_id")
  private Integer groupId;

  private String title;
  private String location;

  @TableField("start_time")
  private Time startTime;

  @TableField("end_time")
  private Time endTime;

  @TableField("checkin_points")
  private Long checkinPoints;

  @TableField("checkin_window_minutes")
  private Integer checkinWindowMinutes;

  @TableField("late_after_minutes")
  private Integer lateAfterMinutes;

  @TableField("late_penalty_points")
  private Long latePenaltyPoints;

  @TableField("absent_penalty_points")
  private Long absentPenaltyPoints;

  private Boolean enabled;

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
