package edu.jmi.openatom.server.openatomsystem.dto.response;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseNotificationDTO {
  private Integer id;
  private String title;
  private String content;
  private String type;
  private Timestamp createdAt;
  private Integer readFlag;
  private Timestamp readAt;
}
