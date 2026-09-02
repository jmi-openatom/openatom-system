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
 * 面试反馈
 *
 * <p>对应数据库表 interview_feedback, 记录面试官对面试者的评分, 建议和评价
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("interview_feedback")
public class InterviewFeedback {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("interview_id")
  private Integer interviewId;

  @TableField("interviewer_id")
  private Integer interviewerId;

  @TableField("template_id")
  private Integer templateId;

  @TableField("template_version")
  private Integer templateVersion;

  private String scores;
  private String details;
  private String suggestion;
  private String comment;
  private String status;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;

  @TableField("submitted_at")
  private Timestamp submittedAt;

  @TableField("withdrawn_at")
  private Timestamp withdrawnAt;
}
