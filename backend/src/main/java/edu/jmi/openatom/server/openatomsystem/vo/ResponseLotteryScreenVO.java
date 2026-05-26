package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 抽奖大屏公开响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLotteryScreenVO {
  private ResponseLotteryVO lottery;
  private List<String> participantNames;
  private List<ResponseLotteryPrizeVO> prizes;
  private List<ResponseLotteryWinnerVO> winners;
  private ResponseLotteryWinnerVO latestWinner;
}
