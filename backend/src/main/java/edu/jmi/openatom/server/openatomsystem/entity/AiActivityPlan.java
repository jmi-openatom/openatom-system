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

/** AI 生成的活动策划案。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_activity_plan")
public class AiActivityPlan {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("session_id")
  private Long sessionId;

  private Integer version;
  private String title;

  @TableField("content_markdown")
  private String contentMarkdown;

  @TableField("structured_fields")
  private String structuredFields;

  private String status;

  @TableField("created_at")
  private Timestamp createdAt;
}
