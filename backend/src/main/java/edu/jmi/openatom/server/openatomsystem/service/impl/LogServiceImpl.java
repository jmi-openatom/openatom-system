package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.mapper.LoginLogMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.OperationLogMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import edu.jmi.openatom.server.openatomsystem.service.LogService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {
  private final OperationLogMapper operationLogMapper;
  private final LoginLogMapper loginLogMapper;

  @Override
  public ApiResponse<List<OperationLog>> getOperationLogs(
      Integer operatorId, String module, String action, String startAt, String endAt) {
    Timestamp startTime = parseTime(startAt);
    Timestamp endTime = parseTime(endAt);

    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(operatorId != null, OperationLog::getOperatorId, operatorId);
    wrapper.eq(module != null && !module.isBlank(), OperationLog::getModule, module);
    wrapper.eq(action != null && !action.isBlank(), OperationLog::getAction, action);
    wrapper.ge(startTime != null, OperationLog::getCreatedAt, startTime);
    wrapper.le(endTime != null, OperationLog::getCreatedAt, endTime);
    wrapper.orderByDesc(OperationLog::getCreatedAt);
    return ApiResponse.success(operationLogMapper.selectList(wrapper));
  }

  @Override
  public ApiResponse<List<LoginLog>> getLoginLogs() {
    return ApiResponse.success(
        loginLogMapper.selectList(
            new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getLoginAt)));
  }

  private Timestamp parseTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().replace("T", " ");
    if (normalized.endsWith("Z")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.length() == 16) {
      normalized = normalized + ":00";
    }
    return Timestamp.valueOf(
        LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }
}
