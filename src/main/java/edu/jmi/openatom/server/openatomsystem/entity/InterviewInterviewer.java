package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("interview_interviewer")
public class InterviewInterviewer {
  @TableField("interview_id")
  private Integer interviewId;

  @TableField("interviewer_id")
  private Integer interviewerId;
}
