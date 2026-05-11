package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import edu.jmi.openatom.server.openatomsystem.service.LogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志控制器
 *
 * <p>提供操作日志和登录日志的查询接口
 */
@RestController
@RequiredArgsConstructor
public class LogController {
  private final LogService logService;

  /**
   * 获取操作日志列表
   *
   * @param operatorId 操作人ID
   * @param module 模块名称
   * @param action 操作动作
   * @param startAt 开始时间
   * @param endAt 结束时间
   * @return 操作日志列表
   */
  @GetMapping("/operation-logs")
  @SaCheckPermission("log:operation:list")
  public Result<List<OperationLog>> operationLogs(
      @RequestParam(required = false) Integer operatorId,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String startAt,
      @RequestParam(required = false) String endAt) {
    return logService.getOperationLogs(operatorId, module, action, startAt, endAt);
  }

  /**
   * 获取登录日志列表
   *
   * @return 登录日志列表
   */
  @GetMapping("/login-logs")
  @SaCheckPermission("log:login:list")
  public Result<List<LoginLog>> loginLogs() {
    return logService.getLoginLogs();
  }
}
