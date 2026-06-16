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

/** AI 活动自动化对话消息。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_activity_message")
public class AiActivityMessage {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("session_id")
  private Long sessionId;

  private String role;
  private String content;

  @TableField("structured_payload")
  private String structuredPayload;

  @TableField("created_at")
  private Timestamp createdAt;
}
