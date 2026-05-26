package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 抽奖活动详情响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLotteryDetailVO {
  private ResponseLotteryVO lottery;
  private List<ResponseLotteryPrizeVO> prizes;
  private List<ResponseLotteryWinnerVO> winners;
}
