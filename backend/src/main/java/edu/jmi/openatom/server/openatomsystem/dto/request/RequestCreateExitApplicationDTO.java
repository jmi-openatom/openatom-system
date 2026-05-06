package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateExitApplicationDTO {
  @NotNull(message = "membershipId不能为空")
  private Integer membershipId;

  @NotBlank(message = "退社原因不能为空")
  private String reason;

  private String description;
}
