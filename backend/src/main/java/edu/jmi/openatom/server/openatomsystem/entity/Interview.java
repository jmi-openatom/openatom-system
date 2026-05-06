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
@AllArgsConstructor
@NoArgsConstructor
@TableName("interview")
public class Interview {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("application_id")
  private Integer applicationId;

  private Integer round;

  @TableField("scheduled_start_at")
  private Timestamp scheduledStartAt;

  @TableField("scheduled_end_at")
  private Timestamp scheduledEndAt;

  private String location;
  private String mode;
  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
