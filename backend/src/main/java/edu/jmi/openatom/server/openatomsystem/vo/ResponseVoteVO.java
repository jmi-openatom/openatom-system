package edu.jmi.openatom.server.openatomsystem.vo;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 投票活动摘要响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseVoteVO {
  private Integer id;
  private Integer clubId;
  private String clubName;
  private String title;
  private String description;
  private String status;
  private String voteType;
  private Integer maxChoices;
  private Boolean anonymousAllowed;
  private Boolean resultVisible;
  private String resultVisibility;
  private Integer optionCount;
  private Integer voterCount;
  private Integer totalVoteCount;
  private Timestamp startAt;
  private Timestamp endAt;
  private Timestamp createdAt;
  private Timestamp updatedAt;
}
