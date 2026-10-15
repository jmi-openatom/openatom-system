package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class RequestUpdateInterviewRoomDTO {
  @NotBlank(message = "请输入面试间名称")
  @Size(max = 80, message = "面试间名称不能超过80个字符")
  private String name;
  @NotBlank(message = "请输入面试地点")
  @Size(max = 255, message = "面试地点不能超过255个字符")
  private String location;
  @NotEmpty(message = "请至少选择一位面试官")
  private List<@NotNull Integer> interviewerIds;
}
