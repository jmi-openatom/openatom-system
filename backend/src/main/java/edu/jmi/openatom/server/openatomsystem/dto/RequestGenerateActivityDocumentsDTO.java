package edu.jmi.openatom.server.openatomsystem.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestGenerateActivityDocumentsDTO {
  private Map<String, Long> templateIds;
  private Map<String, Object> variables;
}
