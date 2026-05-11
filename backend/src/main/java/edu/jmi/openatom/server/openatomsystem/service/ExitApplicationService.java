package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateExitApplicationDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import java.util.List;

/**
 * 退社申请服务接口
 *
 * <p>定义退社申请的列表查询, 创建, 查看详情, 审批通过和驳回等业务操作
 */
public interface ExitApplicationService {
  Result<List<ExitApplication>> list();

  Result<String> create(RequestCreateExitApplicationDTO request);

  Result<ExitApplication> detail(Integer exitApplicationId);

  Result<String> approve(Integer exitApplicationId);

  Result<String> reject(Integer exitApplicationId, String comment);
}
