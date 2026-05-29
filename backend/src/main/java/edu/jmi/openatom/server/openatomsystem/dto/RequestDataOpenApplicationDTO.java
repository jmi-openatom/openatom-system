package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestDataOpenApplicationDTO {
  @NotBlank(message = "应用名称不能为空")
  @Size(max = 120, message = "应用名称不能超过120个字符")
  private String appName;

  @NotBlank(message = "申请人不能为空")
  @Size(max = 80, message = "申请人不能超过80个字符")
  private String applicantName;

  @NotBlank(message = "联系方式不能为空")
  @Size(max = 160, message = "联系方式不能超过160个字符")
  private String applicantContact;

  @Size(max = 160, message = "组织名称不能超过160个字符")
  private String organization;

  @NotBlank(message = "使用场景不能为空")
  @Size(max = 800, message = "使用场景不能超过800个字符")
  private String purpose;
}
