package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePointSummaryVO {
  private ResponsePointAccountVO account;
  private List<ResponsePointTransactionVO> recentTransactions;
  private List<ResponsePointRedemptionVO> redemptions;
}
