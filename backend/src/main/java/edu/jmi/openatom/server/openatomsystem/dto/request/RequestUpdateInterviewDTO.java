package edu.jmi.openatom.server.openatomsystem.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新面试请求
 *
 * <p>用于更新面试安排, 包含面试轮次round, 计划时间scheduledStartAt和scheduledEndAt, 地点location, 面试模式mode, 状态status以及面试官ID列表interviewerIds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateInterviewDTO {
  private Integer round;
  private String scheduledStartAt;
  private String scheduledEndAt;
  private String location;
  private String mode;
  private String status;
  private List<Integer> interviewerIds;
}
