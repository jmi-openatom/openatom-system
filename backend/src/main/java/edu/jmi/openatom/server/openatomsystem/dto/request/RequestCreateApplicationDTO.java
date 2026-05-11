package edu.jmi.openatom.server.openatomsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建申请请求
 *
 * <p>用于用户提交入社申请, 包含招新计划ID campaignId, 社团ID clubId, 第一和第二志愿部门ID firstChoiceDepartmentId和secondChoiceDepartmentId以及个人资料profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestCreateApplicationDTO {
  @NotNull(message = "campaignId不能为空")
  private Integer campaignId;

  @NotNull(message = "clubId不能为空")
  private Integer clubId;

  private Integer firstChoiceDepartmentId;
  private Integer secondChoiceDepartmentId;
  private Map<String, Object> profile;
}
