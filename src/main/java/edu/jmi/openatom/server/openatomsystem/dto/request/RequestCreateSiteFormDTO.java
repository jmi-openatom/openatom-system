package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class RequestCreateSiteFormDTO {
  @NotBlank(message = "表单名称不能为空")
  private String name;

  private String startAt;

  private String endAt;

  private Boolean loginRequired;
  private List<Map<String, Object>> formSchema;

  private String status;
}
