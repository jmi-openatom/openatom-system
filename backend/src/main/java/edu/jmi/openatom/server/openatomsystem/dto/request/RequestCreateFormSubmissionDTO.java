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
public class RequestCreateFormSubmissionDTO {
  private String anonymousName;
  private String anonymousContact;

  @NotNull(message = "表单内容不能为空")
  private Map<String, Object> formData;
}
