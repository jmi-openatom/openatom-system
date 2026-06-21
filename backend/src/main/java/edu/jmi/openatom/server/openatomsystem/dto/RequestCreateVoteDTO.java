package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 创建投票活动请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateVoteDTO {
  @NotBlank(message = "投票标题不能为空")
  private String title;

  private String description;
  private String status;
  private String voteType;
  private Integer maxChoices;
  private Boolean anonymousAllowed;
  private Boolean resultVisible;
  private String resultVisibility;
  private String startAt;
  private String endAt;

  @Valid
  @NotEmpty(message = "请至少配置两个投票选项")
  private List<RequestVoteOptionDTO> options;
}
