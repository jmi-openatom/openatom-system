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
 * 数据开放平台接入申请
 *
 * <p>保存前台提交的开放接口申请、审核状态以及审核通过后生成的调用密钥
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("data_open_application")
public class DataOpenApplication {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("app_name")
  private String appName;

  @TableField("applicant_name")
  private String applicantName;

  @TableField("applicant_contact")
  private String applicantContact;

  private String organization;
  private String purpose;

  @TableField("endpoint_scope")
  private String endpointScope;

  private String status;

  @TableField("api_key")
  private String apiKey;

  @TableField("review_comment")
  private String reviewComment;

  @TableField("reviewed_by")
  private Integer reviewedBy;

  @TableField("reviewed_at")
  private Timestamp reviewedAt;

  @TableField("last_used_at")
  private Timestamp lastUsedAt;

  @TableField("call_count")
  private Integer callCount;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
