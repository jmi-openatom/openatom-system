package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建面试请求
 *
 * <p>用于为申请创建面试安排, 包含申请ID applicationId, 面试轮次round, 计划开始和结束时间scheduledStartAt和scheduledEndAt, 地点location, 面试模式mode以及面试官ID列表interviewerIds
 */
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
