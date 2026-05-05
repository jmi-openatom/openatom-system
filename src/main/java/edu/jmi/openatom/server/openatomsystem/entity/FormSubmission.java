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
@TableName("form_submission")
public class FormSubmission {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("campaign_id")
  private Integer campaignId;

  @TableField("club_id")
  private Integer clubId;

  @TableField("user_id")
  private Integer userId;

  @TableField("anonymous_name")
  private String anonymousName;

  @TableField("anonymous_contact")
  private String anonymousContact;

  @TableField("form_data")
  private String formData;

  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
