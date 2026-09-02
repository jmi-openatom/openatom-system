package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_queue_operation")
public class InterviewQueueOperation {
  @TableId(value = "id", type = IdType.AUTO) private Long id;
  @TableField("session_id") private Integer sessionId;
  @TableField("interview_id") private Integer interviewId;
  @TableField("room_id") private Integer roomId;
  private String action;
  @TableField("operator_id") private Integer operatorId;
  @TableField("detail_json") private String detailJson;
  @TableField("created_at") private Timestamp createdAt;
}
