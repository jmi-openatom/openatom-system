package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import java.util.List;

/**
 * 退社申请服务接口
 *
 * <p>定义退社申请的列表查询, 创建, 查看详情, 审批通过和驳回等业务操作
 */
public interface ExitApplicationService {
  ApiResponse<List<ExitApplication>> list();

  ApiResponse<String> create(RequestCreateExitApplicationDTO request);

  ApiResponse<ExitApplication> detail(Integer exitApplicationId);

  ApiResponse<String> approve(Integer exitApplicationId);

  ApiResponse<String> reject(Integer exitApplicationId, String comment);
}
