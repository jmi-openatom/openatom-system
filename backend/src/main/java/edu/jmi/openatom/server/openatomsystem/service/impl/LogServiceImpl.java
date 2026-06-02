package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.entity.LoginLog;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import edu.jmi.openatom.server.openatomsystem.mapper.LoginLogMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.OperationLogMapper;
import edu.jmi.openatom.server.openatomsystem.service.LogService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
  public Result<PageDataVO<OperationLog>> getOperationLogs(
      Integer operatorId,
      String module,
      String action,
      String startAt,
      String endAt,
      String keyword,
      Long page,
      Long pageSize) {
    Timestamp startTime = parseTime(startAt);
    Timestamp endTime = parseTime(endAt);
    Page<OperationLog> logPage =
        operationLogMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            operatorId,
            module,
            action,
            keyword,
            startTime,
            endTime);
    return Result.success(toPageData(logPage));
  }

  @Override
  public Result<PageDataVO<LoginLog>> getLoginLogs(String keyword, Long page, Long pageSize) {
    Page<LoginLog> logPage =
        loginLogMapper.selectPageByKeyword(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)), keyword);
    return Result.success(toPageData(logPage));
  }

  private <T> PageDataVO<T> toPageData(Page<T> page) {
    return PageDataVO.<T>builder()
        .list(page.getRecords())
        .page(page.getCurrent())
        .pageSize(page.getSize())
        .total(page.getTotal())
        .build();
  }

  private Timestamp parseTime(String value) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim().replace("T", " ");
    if (normalized.endsWith("Z")) normalized = normalized.substring(0, normalized.length() - 1);
    if (normalized.length() == 16) normalized = normalized + ":00";
    return Timestamp.valueOf(
        LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }
}
