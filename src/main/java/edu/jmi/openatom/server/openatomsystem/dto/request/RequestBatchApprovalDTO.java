package edu.jmi.openatom.server.openatomsystem.dto.request;

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
public class RequestBatchApprovalDTO {
  @NotEmpty(message = "applicationIds不能为空")
  private List<Integer> applicationIds;

  private RequestApprovalActionDTO approval;
}
