package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateApplicationDTO {
  @NotNull(message = "campaignId不能为空")
  private Integer campaignId;

  @NotNull(message = "clubId不能为空")
  private Integer clubId;

  private Integer firstChoiceDepartmentId;
  private Integer secondChoiceDepartmentId;
  private Map<String, Object> profile;
}
