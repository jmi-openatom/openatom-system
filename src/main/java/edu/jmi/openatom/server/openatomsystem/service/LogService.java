package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import java.util.List;

public interface LogService {
  ApiResponse<List<OperationLog>> getOperationLogs(
      Integer operatorId, String module, String action, String startAt, String endAt);

  ApiResponse<List<LoginLog>> getLoginLogs();
}
