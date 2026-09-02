package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestMoveInterviewRoomDTO {
  @NotNull private Integer targetRoomId;
}
