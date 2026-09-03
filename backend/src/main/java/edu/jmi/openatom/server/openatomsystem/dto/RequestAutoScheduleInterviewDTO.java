package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RequestAutoScheduleInterviewDTO {
  @NotNull(message = "campaignId不能为空") private Integer campaignId;
  @NotBlank(message = "场次名称不能为空") private String name;
  @NotNull(message = "轮次不能为空") @Min(value = 1, message = "轮次至少为1") private Integer round;
  @NotBlank(message = "开始时间不能为空") private String scheduledStartAt;
  @NotBlank(message = "结束时间不能为空") private String scheduledEndAt;
  @NotNull(message = "单场时长不能为空") @Min(value = 5, message = "单场时长至少5分钟") private Integer durationMinutes;
  @NotNull(message = "间隔不能为空") @Min(value = 0, message = "间隔不能为负数") private Integer gapMinutes;
  private String mode;
  @NotEmpty(message = "请选择候选人") private List<Integer> applicationIds;
  @Valid @NotEmpty(message = "请至少配置一个面试间") private List<Room> rooms;
  /** balanced（默认）或 first_choice_department（按第一志愿部门优先）。 */
  private String assignmentStrategy;
  /** 第一志愿属于这些部门的候选人免面试，直接进入终审候选池。 */
  private List<Integer> skipInterviewDepartmentIds;
  private Map<Integer, Integer> roomAssignments;
  private Boolean previewOnly = true;

  @Data
  public static class Room {
    @NotBlank(message = "面试间名称不能为空") private String name;
    private String location;
    @NotEmpty(message = "每个面试间至少需要一位面试官") private List<Integer> interviewerIds;
    /** 按第一志愿分配时，本面试间优先承接的部门。 */
    private List<Integer> preferredDepartmentIds;
  }
}
