package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新招新状态请求
 *
 * <p>用于更新招新计划的状态, 携带状态值recruitmentStatus（open或closed）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateRecruitmentStatusDTO {
  @NotBlank(message = "招新状态不能为空")
  @Pattern(regexp = "open|closed", message = "招新状态只能是open或closed")
  private String recruitmentStatus;
}
