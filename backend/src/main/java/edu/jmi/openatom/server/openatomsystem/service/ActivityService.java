package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestActivityRegistrationDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateActivityDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ActivityRegistration;
import edu.jmi.openatom.server.openatomsystem.entity.ClubActivity;
import java.util.List;

/**
 * 社团活动服务接口
 *
 * <p>定义社团活动的查询列表, 查看详情, 创建, 更新, 删除以及活动报名和查看报名记录等业务操作
 */
public interface ActivityService {
  ApiResponse<List<ClubActivity>> list(String status);

  ApiResponse<ClubActivity> detail(Integer activityId);

  ApiResponse<String> create(RequestCreateActivityDTO request);

  ApiResponse<String> update(Integer activityId, RequestUpdateActivityDTO request);

  ApiResponse<String> delete(Integer activityId);

  ApiResponse<String> register(Integer activityId, RequestActivityRegistrationDTO request);

  ApiResponse<List<ActivityRegistration>> registrations(Integer activityId);
}
