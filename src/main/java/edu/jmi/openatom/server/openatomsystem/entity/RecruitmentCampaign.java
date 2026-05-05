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
@TableName("recruitment_campaign")
public class RecruitmentCampaign {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("club_id")
  private Integer clubId;

  private String name;

  @TableField("apply_start_at")
  private Timestamp applyStartAt;

  @TableField("apply_end_at")
  private Timestamp applyEndAt;

  @TableField("interview_start_at")
  private Timestamp interviewStartAt;

  @TableField("interview_end_at")
  private Timestamp interviewEndAt;

  @TableField("result_publish_at")
  private Timestamp resultPublishAt;

  @TableField("target_grades")
  private String targetGrades;

  @TableField("max_applicants")
  private Integer maxApplicants;

  @TableField("login_required")
  private Boolean loginRequired;

  @TableField("form_schema")
  private String formSchema;

  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
