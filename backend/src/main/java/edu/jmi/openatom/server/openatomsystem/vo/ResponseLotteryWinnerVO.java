package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 抽奖中奖记录响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLotteryWinnerVO {
  private Integer id;
  private Integer lotteryId;
  private Integer prizeId;
  private String prizeName;
  private String prizeLevel;
  private Integer submissionId;
  private Integer userId;
  private String winnerName;
  private String winnerContact;
  private String winnerAccount;
  private Map<String, Object> formData;
  private Timestamp wonAt;
}
