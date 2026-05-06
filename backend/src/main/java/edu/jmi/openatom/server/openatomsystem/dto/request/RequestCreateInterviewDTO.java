package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateInterviewDTO {
  @NotNull(message = "applicationId不能为空")
  private Integer applicationId;

  @NotNull(message = "面试轮次不能为空")
  private Integer round;

  @NotBlank(message = "面试开始时间不能为空")
  private String scheduledStartAt;

  @NotBlank(message = "面试结束时间不能为空")
  private String scheduledEndAt;

  private String location;
  private String mode;
  private List<Integer> interviewerIds;
}
