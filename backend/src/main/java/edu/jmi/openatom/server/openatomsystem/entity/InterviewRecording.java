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

/** 面试原始录音及浏览器实时转写。 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("interview_recording")
public class InterviewRecording {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  @TableField("interview_id")
  private Integer interviewId;

  @TableField("interviewer_id")
  private Integer interviewerId;

  @TableField("file_name")
  private String fileName;

  @TableField("mime_type")
  private String mimeType;

  @TableField("file_size")
  private Long fileSize;

  @TableField("duration_seconds")
  private Integer durationSeconds;

  private String transcript;

  @TableField("transcript_updated_at")
  private Timestamp transcriptUpdatedAt;

  @TableField("created_at")
  private Timestamp createdAt;
}
