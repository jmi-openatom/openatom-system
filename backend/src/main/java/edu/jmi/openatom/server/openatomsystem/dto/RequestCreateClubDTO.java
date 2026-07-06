package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建社团请求
 *
 * <p>用于创建新社团, 包含社团名称name, 唯一编码code, 分类category, 描述description, Logo地址logoUrl和社长用户ID presidentUserId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateClubDTO {
  @NotBlank(message = "社团名称不能为空")
  @Size(max = 64, message = "社团名称长度不能超过64个字符")
  private String name;

  @NotBlank(message = "社团编码不能为空")
  @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$", message = "社团编码只能包含小写字母、数字、下划线和连字符，并以字母开头")
  private String code;

  @Size(max = 64, message = "社团分类长度不能超过64个字符")
  private String category;

  @Size(max = 1000, message = "社团描述长度不能超过1000个字符")
  private String description;

  private String logoUrl;
  private Integer presidentUserId;
  private Integer vicePresidentUserId;
  private Integer leagueSecretaryUserId;
}
