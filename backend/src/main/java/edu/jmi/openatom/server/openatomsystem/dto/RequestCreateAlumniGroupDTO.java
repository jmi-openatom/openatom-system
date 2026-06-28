package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建往届管理人员分组请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateAlumniGroupDTO {
  @NotBlank(message = "分组名称不能为空")
  private String name;
  private String description;
  private Integer sortOrder;
}
