package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class RequestCheckInTargetsDTO {
  @NotEmpty(message = "请选择人员")
  private List<Integer> userIds;
}
