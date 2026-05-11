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
 * 操作日志
 *
 * <p>对应数据库表 operation_log, 记录用户在系统中的操作行为, 包括操作模块, 动作和内容
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("operation_log")
public class OperationLog {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("operator_id")
  private Integer operatorId;

  private String module;
  private String action;

  @TableField("target_id")
  private String targetId;

  private String content;

  @TableField("created_at")
  private Timestamp createdAt;
}
