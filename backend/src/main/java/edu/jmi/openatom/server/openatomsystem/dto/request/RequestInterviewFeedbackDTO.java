package edu.jmi.openatom.server.openatomsystem.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestInterviewFeedbackDTO {
  private Map<String, Object> scores;
  private String suggestion;
  private String comment;
}
