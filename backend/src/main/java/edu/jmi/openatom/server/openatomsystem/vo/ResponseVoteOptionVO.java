package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 投票选项响应 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseVoteOptionVO {
  private Integer id;
  private Integer voteId;
  private String title;
  private String description;
  private Integer sortOrder;
  private String color;
  private Integer voteCount;
  private Double votePercent;
}
