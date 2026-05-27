package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotBlank;
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
  private Integer groupId;

  @NotBlank(message = "签到标题不能为空")
  private String title;

  private String location;
  private String startAt;
  private String endAt;
  private String status;
  private Integer checkinPoints;

  private List<Integer> targetUserIds;
}
