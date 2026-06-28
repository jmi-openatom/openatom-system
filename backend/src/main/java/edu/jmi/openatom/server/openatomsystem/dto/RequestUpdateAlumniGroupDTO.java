package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新往届管理人员分组请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateAlumniGroupDTO {
  private String name;
  private String description;
  private Integer sortOrder;
}
