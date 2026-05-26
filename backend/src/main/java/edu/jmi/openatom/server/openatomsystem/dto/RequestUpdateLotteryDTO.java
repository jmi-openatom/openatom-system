package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 更新抽奖活动请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateLotteryDTO {
  private Integer formId;
  private String title;
  private String description;
  private String status;

  @Valid private List<RequestLotteryPrizeDTO> prizes;
}
