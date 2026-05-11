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
 * 活动报名记录
 *
 * <p>对应数据库表 activity_registration, 记录用户对社团活动的报名信息, 包含报名表单数据和审批状态
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("activity_registration")
public class ActivityRegistration {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("activity_id")
  private Integer activityId;

  @TableField("user_id")
  private Integer userId;

  @TableField("form_data")
  private String formData;

  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
