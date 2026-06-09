package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

  @TableField("created_by")
  private Integer createdBy;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
