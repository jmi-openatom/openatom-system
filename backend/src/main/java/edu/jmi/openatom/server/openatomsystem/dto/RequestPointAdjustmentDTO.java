package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestPointAdjustmentDTO {
  @NotNull(message = "请选择用户")
  private Integer userId;

  @NotNull(message = "请输入调整积分")
  private Long delta;

  private String description;
}
