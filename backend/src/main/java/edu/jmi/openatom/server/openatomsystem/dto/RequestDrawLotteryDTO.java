package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 执行抽奖请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDrawLotteryDTO {
  private Integer prizeId;
}
