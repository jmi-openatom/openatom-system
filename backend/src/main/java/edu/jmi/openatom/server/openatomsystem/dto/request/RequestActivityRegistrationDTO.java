package edu.jmi.openatom.server.openatomsystem.dto.request;

import lombok.Data;

/**
 * 活动报名请求
 *
 * <p>用于用户在活动报名时提交自定义表单数据, 携带formData字段保存报名表单内容
 */
@Data
public class RequestActivityRegistrationDTO {
  private Object formData;
}
