package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建奖项请求
 *
 * <p>用于创建获奖记录, 包含奖项标题title, 比赛名称competitionName, 获奖级别awardLevel, 获奖年份awardYear, 团队名称teamName, 描述description和排序sortOrder
 */
@Data
public class RequestCreateAwardDTO {
  @NotBlank(message = "获奖标题不能为空")
  private String title;

  @NotBlank(message = "比赛名称不能为空")
  private String competitionName;

  private String awardLevel;
  private Integer awardYear;
  private String teamName;
  private String description;
  private Integer sortOrder;
}
