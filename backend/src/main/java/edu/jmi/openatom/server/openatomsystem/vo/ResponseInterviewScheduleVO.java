package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseInterviewScheduleVO {
  private Integer sessionId;
  private String sessionName;
  private String status;
  private Integer totalCandidates;
  private List<Room> rooms;
  private List<Assignment> assignments;

  @Data
  @Builder
  public static class Room {
    private Integer roomId;
    private String name;
    private String location;
    private Integer capacity;
    private Integer assignedCount;
    private List<Interviewer> interviewers;
  }

  @Data
  @Builder
  public static class Interviewer {
    private Integer userId;
    private String name;
  }

  @Data
  @Builder
  public static class Assignment {
    private Integer interviewId;
    private Integer applicationId;
    private String applicantName;
    private Integer roomIndex;
    private Integer roomId;
    private String roomName;
    private Integer queueNumber;
    private Timestamp scheduledStartAt;
    private Timestamp scheduledEndAt;
  }
}
