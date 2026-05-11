package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 社团主页VO
 *
 * <p>封装社团主页展示所需的数据, 包括社团基本信息, 统计数据, 特色领域, 近期活动, 成员风采, 荣誉奖项及技术栈
 */
@Data
@Builder
public class ResponseClubHomeVO {
  private ClubProfile club;
  private List<Metric> metrics;
  private List<FocusArea> focusAreas;
  private List<Activity> activities;
  private List<Person> people;
  private List<Award> awards;
  private List<String> techStack;

  @Data
  @Builder
  public static class ClubProfile {
    private Integer id;
    private String name;
    private String code;
    private String category;
    private String description;
    private String logoUrl;
    private String recruitmentStatus;
  }

  @Data
  @Builder
  public static class Metric {
    private String label;
    private String value;
    private String note;
  }

  @Data
  @Builder
  public static class FocusArea {
    private String title;
    private String description;
    private String icon;
  }

  @Data
  @Builder
  public static class Activity {
    private Integer id;
    private String date;
    private String title;
    private String description;
    private String location;
    private String status;
    private String coverUrl;
  }

  @Data
  @Builder
  public static class Person {
    private Integer userId;
    private String name;
    private String initial;
    private String role;
    private String focus;
    private String avatar;
  }

  @Data
  @Builder
  public static class Award {
    private Integer id;
    private Integer year;
    private String title;
    private String competitionName;
    private String awardLevel;
    private String teamName;
    private String description;
  }
}
