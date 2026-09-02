package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseInterviewQueueVO {
  private Integer sessionId;
  private String sessionName;
  private String sessionStatus;
  private Timestamp serverTime;
  private Stats stats;
  private List<Room> rooms;
  private List<Candidate> candidates;

  @Data
  @Builder
  public static class Stats {
    private Integer total;
    private Integer checkedIn;
    private Integer waiting;
    private Integer called;
    private Integer completed;
    private Integer noShow;
    private Integer notCheckedIn;
  }

  @Data
  @Builder
  public static class Room {
    private Integer roomId;
    private String name;
    private String location;
    private Integer waitingCount;
    private Candidate current;
  }

  @Data
  @Builder
  public static class Candidate {
    private Integer interviewId;
    private Integer applicationId;
    private Integer roomId;
    private String roomName;
    private Integer queueNumber;
    private String applicantName;
    private String studentId;
    private Timestamp scheduledStartAt;
    private String interviewStatus;
    private String queueStatus;
    private Timestamp checkedInAt;
    private Timestamp calledAt;
    private Integer callCount;
  }
}
