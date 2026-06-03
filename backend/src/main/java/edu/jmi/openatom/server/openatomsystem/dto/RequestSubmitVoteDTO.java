package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 提交投票请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSubmitVoteDTO {
  @NotEmpty(message = "请选择投票选项")
  private List<Integer> optionIds;

  private String voterName;
  private String voterContact;
  private String remark;
}
