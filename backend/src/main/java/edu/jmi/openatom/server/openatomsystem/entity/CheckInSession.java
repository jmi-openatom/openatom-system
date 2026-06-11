package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Date;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 内部签到场次 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("checkin_session")
public class CheckInSession {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("activity_id")
  private Integer activityId;

  @TableField("group_id")
  private Integer groupId;

  @TableField("schedule_id")
  private Integer scheduleId;

  @TableField("session_type")
  private String sessionType;

  @TableField("attendance_date")
  private Date attendanceDate;

  private String title;
  private String location;

  @TableField("start_at")
  private Timestamp startAt;

  @TableField("end_at")
  private Timestamp endAt;

  private String status;
  private String token;

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

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
