package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.Data;

/**
 * 更新奖项请求
 *
 * <p>用于更新获奖记录, 包含奖项标题title, 比赛名称competitionName, 获奖级别awardLevel, 获奖年份awardYear, 团队名称teamName, 描述description和排序sortOrder
 */
@Data
public class RequestUpdateAwardDTO {
  private String title;
  private String competitionName;
  private String awardLevel;
  private Integer awardYear;
  private String teamName;
  private String description;
  private Integer sortOrder;
}
