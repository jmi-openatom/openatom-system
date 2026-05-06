package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import edu.jmi.openatom.server.openatomsystem.service.LogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LogController {
  private final LogService logService;

  @GetMapping("/operation-logs")
  @SaCheckPermission("log:operation:list")
  public ApiResponse<List<OperationLog>> operationLogs(
      @RequestParam(required = false) Integer operatorId,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String startAt,
      @RequestParam(required = false) String endAt) {
    return logService.getOperationLogs(operatorId, module, action, startAt, endAt);
  }

  @GetMapping("/login-logs")
  @SaCheckPermission("log:login:list")
  public ApiResponse<List<LoginLog>> loginLogs() {
    return logService.getLoginLogs();
  }
}
