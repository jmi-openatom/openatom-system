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

/** AI 活动自动化会话。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_activity_session")
public class AiActivitySession {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("club_id")
  private Integer clubId;

  @TableField("user_id")
  private Integer userId;

  private String title;
  private String status;

  @TableField("requirement_summary")
  private String requirementSummary;

  @TableField("confirmed_plan_id")
  private Long confirmedPlanId;

  @TableField("activity_id")
  private Integer activityId;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
