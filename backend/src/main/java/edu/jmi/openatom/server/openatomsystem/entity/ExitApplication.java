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
 * 退社申请
 *
 * <p>对应数据库表 exit_application, 记录成员的退社申请信息, 包含退社原因和审批状态
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("exit_application")
public class ExitApplication {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("membership_id")
  private Integer membershipId;

  private String reason;
  private String description;
  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
