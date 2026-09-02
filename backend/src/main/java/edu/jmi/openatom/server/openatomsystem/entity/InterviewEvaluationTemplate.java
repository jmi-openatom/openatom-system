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
@TableName("interview_evaluation_template")
public class InterviewEvaluationTemplate {
  @TableId(value = "id", type = IdType.AUTO) private Integer id;
  @TableField("campaign_id") private Integer campaignId;
  private String name;
  @TableField("schema_json") private String schemaJson;
  private Integer version;
  private String status;
  @TableField("created_by") private Integer createdBy;
  @TableField("created_at") private Timestamp createdAt;
  @TableField("updated_at") private Timestamp updatedAt;
}
