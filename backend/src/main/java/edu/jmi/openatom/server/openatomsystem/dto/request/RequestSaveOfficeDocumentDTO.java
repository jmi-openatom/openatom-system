package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class RequestSaveOfficeDocumentDTO {
  @NotBlank(message = "单据类型不能为空")
  private String docType;

  @NotBlank(message = "标题不能为空")
  private String title;

  @NotNull(message = "单据内容不能为空")
  private Map<String, Object> payload;
}
