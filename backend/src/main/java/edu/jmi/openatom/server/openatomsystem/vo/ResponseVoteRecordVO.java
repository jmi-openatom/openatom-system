package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 投票记录响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseVoteRecordVO {
  private Integer id;
  private Integer voteId;
  private Integer userId;
  private String voterName;
  private String voterContact;
  private List<Integer> optionIds;
  private List<String> optionTitles;
  private String remark;
  private Timestamp votedAt;
}
