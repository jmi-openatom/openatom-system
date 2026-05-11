package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试官关联
 *
 * <p>对应数据库表 interview_interviewer, 建立面试与面试官之间的多对多关联关系
 */
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
