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

/** 积分流水 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("point_transaction")
public class PointTransaction {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("user_id")
  private Integer userId;

  private Long delta;

  @TableField("balance_after")
  private Long balanceAfter;

  private String type;

  @TableField("source_type")
  private String sourceType;

  @TableField("source_id")
  private Integer sourceId;

  @TableField("source_key")
  private String sourceKey;

  private String description;

  @TableField("operator_id")
  private Integer operatorId;

  @TableField("created_at")
  private Timestamp createdAt;
}
