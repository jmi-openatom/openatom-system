package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestActivityRegistrationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;

/**
 * 社团活动服务接口
 *
 * <p>定义社团活动的查询列表, 查看详情, 创建, 更新, 删除以及活动报名和查看报名记录等业务操作
 */
public interface ActivityService {
  Result<List<ClubActivity>> list(String status);

  Result<ClubActivity> detail(Integer activityId);

  Result<String> create(RequestCreateActivityDTO request);

  Result<String> update(Integer activityId, RequestUpdateActivityDTO request);

  Result<String> delete(Integer activityId);

  Result<String> register(Integer activityId, RequestActivityRegistrationDTO request);

  Result<List<ActivityRegistration>> registrations(Integer activityId);
}
