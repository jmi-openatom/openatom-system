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

/** AI 调用日志。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_call_log")
public class AiCallLog {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Integer userId;

  private String scene;
  private String provider;
  private String model;

  @TableField("prompt_version")
  private String promptVersion;

  @TableField("request_summary")
  private String requestSummary;

  @TableField("response_summary")
  private String responseSummary;

  private String status;

  @TableField("error_message")
  private String errorMessage;

  @TableField("duration_ms")
  private Long durationMs;

  @TableField("created_at")
  private Timestamp createdAt;
}
