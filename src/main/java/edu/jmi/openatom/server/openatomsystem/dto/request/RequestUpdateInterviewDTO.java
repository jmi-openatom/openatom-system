package edu.jmi.openatom.server.openatomsystem.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateInterviewDTO {
  private Integer round;
  private String scheduledStartAt;
  private String scheduledEndAt;
  private String location;
  private String mode;
  private String status;
  private List<Integer> interviewerIds;
}
