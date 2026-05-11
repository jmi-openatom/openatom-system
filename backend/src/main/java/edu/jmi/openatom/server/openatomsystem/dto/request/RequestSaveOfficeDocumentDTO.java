package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存办公单据请求
 *
 * <p>用于保存办公流程单据, 包含单据类型docType, 标题title和单据内容payload
 */
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
