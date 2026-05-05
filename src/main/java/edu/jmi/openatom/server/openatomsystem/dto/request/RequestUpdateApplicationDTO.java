package edu.jmi.openatom.server.openatomsystem.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateApplicationDTO {
  private Integer firstChoiceDepartmentId;
  private Integer secondChoiceDepartmentId;
  private Map<String, Object> profile;
}
