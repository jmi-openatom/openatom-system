package edu.jmi.openatom.server.openatomsystem.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 投票活动详情响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseVoteDetailVO {
  private ResponseVoteVO vote;
  private List<ResponseVoteOptionVO> options;
  private List<ResponseVoteRecordVO> records;
  private Boolean voted;
}
