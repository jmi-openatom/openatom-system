package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 抽奖奖品配置请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLotteryPrizeDTO {
  private Integer id;

  @NotBlank(message = "奖品名称不能为空")
  private String name;

  private String level;

  @NotNull(message = "奖品数量不能为空")
  @Min(value = 1, message = "奖品数量必须大于0")
  private Integer quantity;

  private Integer sortOrder;

  private String color;
}
