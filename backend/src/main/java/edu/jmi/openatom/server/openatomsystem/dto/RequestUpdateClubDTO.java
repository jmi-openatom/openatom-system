package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新社团请求
 *
 * <p>用于更新社团信息, 包含社团名称name, 分类category, 描述description, Logo地址logoUrl和社长用户ID presidentUserId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateClubDTO {
  @Size(max = 64, message = "社团名称长度不能超过64个字符")
  private String name;

  @Size(max = 64, message = "社团分类长度不能超过64个字符")
  private String category;

  @Size(max = 1000, message = "社团描述长度不能超过1000个字符")
  private String description;

  private String logoUrl;
  private Integer presidentUserId;
}
