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
@TableName("interview_room")
public class InterviewRoom {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;
  @TableField("session_id") private Integer sessionId;
  private String name;
  private String location;
  @TableField("sort_order") private Integer sortOrder;
  @TableField("created_at") private Timestamp createdAt;
}
