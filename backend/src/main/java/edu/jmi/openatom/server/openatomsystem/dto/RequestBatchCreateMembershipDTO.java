package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class RequestBatchCreateMembershipDTO {
  @NotEmpty(message = "成员列表不能为空")
  private List<MembershipItem> memberships;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MembershipItem {
    @NotNull(message = "userId不能为空")
    private Integer userId;

    @NotNull(message = "clubId不能为空")
    private Integer clubId;

    private Integer departmentId;
    private Integer positionId;
    private String status;
    private Boolean featured;
    private Integer sortOrder;
  }
}
