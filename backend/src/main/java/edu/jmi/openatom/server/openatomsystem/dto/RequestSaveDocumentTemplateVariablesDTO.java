package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSaveDocumentTemplateVariablesDTO {
  @NotNull(message = "模板变量不能为空")
  private List<Map<String, Object>> variables;
}
