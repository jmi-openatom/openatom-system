package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 抽奖奖品响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLotteryPrizeVO {
  private Integer id;
  private Integer lotteryId;
  private String name;
  private String level;
  private Integer quantity;
  private Integer sortOrder;
  private String color;
  private Integer wonCount;
  private Integer remainingCount;
}
