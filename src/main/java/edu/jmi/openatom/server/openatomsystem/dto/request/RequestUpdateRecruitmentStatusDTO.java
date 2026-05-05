package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateRecruitmentStatusDTO {
  @NotBlank(message = "招新状态不能为空")
  @Pattern(regexp = "open|closed", message = "招新状态只能是open或closed")
  private String recruitmentStatus;
}
