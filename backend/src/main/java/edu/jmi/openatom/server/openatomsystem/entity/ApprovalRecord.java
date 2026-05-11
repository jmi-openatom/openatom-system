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
 * 审批记录
 *
 * <p>对应数据库表 approval_record, 记录审批流程中每个节点的操作信息, 包括审批人, 动作和备注
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("approval_record")
public class ApprovalRecord {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("application_id")
  private Integer applicationId;

  private String node;
  private String action;

  @TableField("operator_id")
  private Integer operatorId;

  private String comment;

  @TableField("created_at")
  private Timestamp createdAt;
}
