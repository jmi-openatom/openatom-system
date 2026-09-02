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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_session")
public class InterviewSession {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;
  @TableField("campaign_id") private Integer campaignId;
  private String name;
  private Integer round;
  @TableField("scheduled_start_at") private Timestamp scheduledStartAt;
  @TableField("scheduled_end_at") private Timestamp scheduledEndAt;
  @TableField("duration_minutes") private Integer durationMinutes;
  @TableField("gap_minutes") private Integer gapMinutes;
  private String mode;
  private String status;
  @TableField("created_by") private Integer createdBy;
  @TableField("created_at") private Timestamp createdAt;
  @TableField("updated_at") private Timestamp updatedAt;
}
