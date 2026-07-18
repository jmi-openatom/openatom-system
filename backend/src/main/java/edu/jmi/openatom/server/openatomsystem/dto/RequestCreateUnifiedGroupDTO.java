package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class RequestCreateUnifiedGroupDTO {
  @NotNull(message = "clubId不能为空")
  private Integer clubId;

  @NotBlank(message = "分组类型不能为空")
  private String type;

  @NotBlank(message = "分组名称不能为空")
  @Size(max = 160, message = "分组名称不能超过160个字符")
  private String name;

  @Size(max = 500, message = "分组描述不能超过500个字符")
  private String description;

  private Integer sortOrder;
  private List<Integer> userIds;
}
