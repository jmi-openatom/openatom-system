package edu.jmi.openatom.lab.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_submissions")
public class LabSubmission {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private Long problemId;
  private String language;
  private String code;
  private String judgeStatus;
  private Integer scorePassed;
  private Integer totalCases;
  private Integer runtimeMs;
  private Integer memoryKb;
  private String errorMessage;
  private LocalDateTime submittedAt;
  private LocalDateTime judgedAt;
}
