package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateCheckInSessionDTO {
  private Integer activityId;

  @NotBlank(message = "签到标题不能为空")
  private String title;

  private String location;
  private String startAt;
  private String endAt;
  private String status;

  @NotEmpty(message = "请选择发放人员")
  private List<Integer> targetUserIds;
}
