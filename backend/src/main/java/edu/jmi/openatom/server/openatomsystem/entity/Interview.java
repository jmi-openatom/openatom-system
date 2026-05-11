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

/**
 * 面试记录
 *
 * <p>对应数据库表 interview, 记录招新面试的调度信息, 包括面试时间, 地点, 方式和状态
 */
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
