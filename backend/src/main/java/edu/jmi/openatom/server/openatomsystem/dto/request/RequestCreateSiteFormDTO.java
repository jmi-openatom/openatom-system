package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建站点表单请求
 *
 * <p>用于创建站点收集表单, 包含表单名称name, 填写起止时间startAt和endAt, 是否要求登录loginRequired, 表单结构formSchema和状态status
 */
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
