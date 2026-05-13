package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class RequestCheckInGroupDTO {
  @NotBlank(message = "组名称不能为空")
  private String name;

  private List<Integer> userIds;
}
