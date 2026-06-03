package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 更新投票活动请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateVoteDTO {
  private String title;
  private String description;
  private String status;
  private String voteType;
  private Integer maxChoices;
  private Boolean anonymousAllowed;
  private Boolean resultVisible;
  private String startAt;
  private String endAt;

  @Valid private List<RequestVoteOptionDTO> options;
}
