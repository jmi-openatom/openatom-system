package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 保存社团规章制度请求 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestSaveClubRegulationDTO {
  @NotBlank(message = "制度标题不能为空")
  @Size(max = 200, message = "制度标题不能超过200个字符")
  private String title;

  @Size(max = 800, message = "制度摘要不能超过800个字符")
  private String summary;

  @NotBlank(message = "制度正文不能为空")
  private String contentMarkdown;

  private String status;
  private Integer sortOrder;
}
