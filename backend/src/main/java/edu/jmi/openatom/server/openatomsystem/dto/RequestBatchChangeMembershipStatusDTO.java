package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestBatchChangeMembershipStatusDTO {
  @NotEmpty(message = "成员ID列表不能为空")
  private List<Integer> membershipIds;

  @NotBlank(message = "成员状态不能为空")
  private String status;
}
