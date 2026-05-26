package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 抽奖活动摘要响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseLotteryVO {
  private Integer id;
  private Integer clubId;
  private String clubName;
  private Integer formId;
  private String formName;
  private String title;
  private String description;
  private String status;
  private Integer participantCount;
  private Integer winnerCount;
  private Integer totalPrizeCount;
  private Integer remainingPrizeCount;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
