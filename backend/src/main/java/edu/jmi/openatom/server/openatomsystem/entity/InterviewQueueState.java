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
@TableName("interview_queue_state")
public class InterviewQueueState {
  @TableId(value = "id", type = IdType.AUTO) private Long id;
  @TableField("interview_id") private Integer interviewId;
  @TableField("session_id") private Integer sessionId;
  @TableField("room_id") private Integer roomId;
  private String status;
  @TableField("checked_in_by") private Integer checkedInBy;
  @TableField("checked_in_at") private Timestamp checkedInAt;
  @TableField("called_at") private Timestamp calledAt;
  @TableField("call_count") private Integer callCount;
  @TableField("updated_at") private Timestamp updatedAt;
}
