package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 创建抽奖活动请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateLotteryDTO {
  @NotNull(message = "关联表单不能为空")
  private Integer formId;

  @NotBlank(message = "抽奖标题不能为空")
  private String title;

  private String description;

  private String status;

  @Valid
  @NotEmpty(message = "请至少配置一个奖品")
  private List<RequestLotteryPrizeDTO> prizes;
}
