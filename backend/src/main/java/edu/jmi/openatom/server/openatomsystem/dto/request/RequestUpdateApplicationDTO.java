package edu.jmi.openatom.server.openatomsystem.dto.request;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新申请请求
 *
 * <p>用于用户修改已提交的入社申请, 包含第一和第二志愿部门ID firstChoiceDepartmentId和secondChoiceDepartmentId以及个人资料profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateApplicationDTO {
  private Integer firstChoiceDepartmentId;
  private Integer secondChoiceDepartmentId;
  private Map<String, Object> profile;
}
