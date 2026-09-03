package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 候选人在现场签到页提交的学号。 */
@Data
public class RequestInterviewSelfCheckInDTO {
  @NotBlank(message = "请输入学号")
  @Size(max = 32, message = "学号长度不能超过32个字符")
  private String studentId;
}
