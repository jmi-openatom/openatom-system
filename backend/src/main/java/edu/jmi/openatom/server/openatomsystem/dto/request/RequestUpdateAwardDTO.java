package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.Data;

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
