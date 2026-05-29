package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestReviewDataOpenApplicationDTO {
  @NotBlank(message = "审核状态不能为空")
  private String status;

  @Size(max = 500, message = "审核意见不能超过500个字符")
  private String reviewComment;
}
