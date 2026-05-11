package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建表单提交请求
 *
 * <p>用于提交站点表单数据, 支持匿名提交时填写anonymousName和anonymousContact, 携带表单内容formData
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateFormSubmissionDTO {
  private String anonymousName;
  private String anonymousContact;

  @NotNull(message = "表单内容不能为空")
  private Map<String, Object> formData;
}
