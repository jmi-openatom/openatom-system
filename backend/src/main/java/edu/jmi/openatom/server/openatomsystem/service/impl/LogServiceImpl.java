package edu.jmi.openatom.server.openatomsystem.service.impl;

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

/**
 * 日志管理实现类
 *
 * <p>负责操作日志和登录日志的查询, 支持按操作人, 模块, 时间和行为等条件筛选
 */
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
    return ApiResponse.success(
        operationLogMapper.selectByConditions(operatorId, module, action, startTime, endTime));
  }

  @Override
  public ApiResponse<List<LoginLog>> getLoginLogs() {
    return ApiResponse.success(loginLogMapper.selectAllOrdered());
  }

  private Timestamp parseTime(String value) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim().replace("T", " ");
    if (normalized.endsWith("Z")) normalized = normalized.substring(0, normalized.length() - 1);
    if (normalized.length() == 16) normalized = normalized + ":00";
    return Timestamp.valueOf(LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }
}
