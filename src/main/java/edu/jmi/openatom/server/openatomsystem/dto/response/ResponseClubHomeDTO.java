package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseClubHomeDTO {
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
