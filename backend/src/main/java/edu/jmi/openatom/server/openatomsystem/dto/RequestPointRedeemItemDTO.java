package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestPointRedeemItemDTO {
  @NotBlank(message = "兑换项名称不能为空")
  private String name;

  private String description;

  @NotNull(message = "兑换所需积分不能为空")
  private Long pointCost;

  private Integer stock;
  private String status;
  private Integer sortOrder;
  private String imageUrl;
}
