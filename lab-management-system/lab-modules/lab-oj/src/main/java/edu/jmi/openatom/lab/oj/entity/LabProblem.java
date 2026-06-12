package edu.jmi.openatom.lab.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_problems")
public class LabProblem {
  @TableId(type = IdType.AUTO)
  private Long id;

  private LocalDate problemDate;
  private String title;
  private String slug;
  private String difficulty;
  private String descriptionMarkdown;
  private Integer timeLimitMs;
  private Integer memoryLimitMb;
  private String status;
  private Boolean aiGenerated;
  private String solutionCpp;
  private String solutionJava;
  private String solutionPython;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
