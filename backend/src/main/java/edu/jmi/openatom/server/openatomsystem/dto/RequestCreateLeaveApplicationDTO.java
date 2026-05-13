package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 创建请假申请请求 */
@Data
public class RequestCreateLeaveApplicationDTO {
  @NotBlank(message = "请假标题不能为空")
  private String title;

  @NotBlank(message = "请假理由不能为空")
  private String reason;

  private String startAt;
  private String endAt;

  @NotEmpty(message = "请上传请假附件")
  private List<Map<String, Object>> attachments;
}
