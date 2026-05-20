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

/** 请假申请 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("leave_application")
public class LeaveApplication {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("user_id")
  private Integer userId;

  @TableField("club_id")
  private Integer clubId;

  private String title;
  private String reason;

  @TableField("start_at")
  private Timestamp startAt;

  @TableField("end_at")
  private Timestamp endAt;

  private String attachments;
  private String status;

  @TableField("reviewer_id")
  private Integer reviewerId;

  @TableField("review_comment")
  private String reviewComment;

  @TableField("reviewed_at")
  private Timestamp reviewedAt;

  @TableField("bot_notify_origin")
  private String botNotifyOrigin;

  @TableField("bot_notify_user_id")
  private String botNotifyUserId;

  @TableField("bot_notified_at")
  private Timestamp botNotifiedAt;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
