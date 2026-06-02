package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;

/**
 * 日志查询服务接口
 *
 * <p>定义操作日志和登录日志的查询等业务操作
 */
public interface LogService {
  Result<PageDataVO<OperationLog>> getOperationLogs(
      Integer operatorId,
      String module,
      String action,
      String startAt,
      String endAt,
      String keyword,
      Long page,
      Long pageSize);

  Result<PageDataVO<LoginLog>> getLoginLogs(String keyword, Long page, Long pageSize);
}
