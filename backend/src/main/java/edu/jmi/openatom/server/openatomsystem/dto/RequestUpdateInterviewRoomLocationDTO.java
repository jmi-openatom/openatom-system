package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestUpdateInterviewRoomLocationDTO {
  @NotBlank(message = "请输入面试地点")
  @Size(max = 255, message = "面试地点不能超过255个字符")
  private String location;
}
