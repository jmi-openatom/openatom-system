package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import java.util.List;

/**
 * 日志查询服务接口
 *
 * <p>定义操作日志和登录日志的查询等业务操作
 */
public interface LogService {
  ApiResponse<List<OperationLog>> getOperationLogs(
      Integer operatorId, String module, String action, String startAt, String endAt);

  ApiResponse<List<LoginLog>> getLoginLogs();
}
