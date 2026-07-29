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
@TableName("mailbox_outbox_event")
public class MailboxOutboxEvent {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("event_id")
  private String eventId;

  @TableField("event_type")
  private String eventType;

  @TableField("aggregate_id")
  private String aggregateId;

  @TableField("payload_json")
  private String payloadJson;

  private String status;

  @TableField("retry_count")
  private Integer retryCount;

  @TableField("next_retry_at")
  private Timestamp nextRetryAt;

  @TableField("processing_started_at")
  private Timestamp processingStartedAt;

  @TableField("last_error")
  private String lastError;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("processed_at")
  private Timestamp processedAt;
}
