package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import java.sql.Timestamp;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志数据访问层
 *
 * <p>提供对操作日志(OperationLog)的数据库操作, 包括按操作人, 模块, 动作和时间范围等条件查询日志等功能
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

  /** 按条件查操作日志 */
  default Page<OperationLog> selectPageByConditions(
      Page<OperationLog> page,
      Integer operatorId, String module, String action, String keyword,
      Timestamp startTime, Timestamp endTime) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    String trimmedKeyword = keyword == null ? null : keyword.trim();
    Integer keywordOperatorId = parseInteger(trimmedKeyword);
    wrapper.eq(operatorId != null, OperationLog::getOperatorId, operatorId);
    wrapper.eq(module != null && !module.isBlank(), OperationLog::getModule, module);
    wrapper.eq(action != null && !action.isBlank(), OperationLog::getAction, action);
    wrapper.ge(startTime != null, OperationLog::getCreatedAt, startTime);
    wrapper.le(endTime != null, OperationLog::getCreatedAt, endTime);
    wrapper.and(
        trimmedKeyword != null && !trimmedKeyword.isBlank(),
        query -> {
          query
              .like(OperationLog::getModule, trimmedKeyword)
              .or()
              .like(OperationLog::getAction, trimmedKeyword)
              .or()
              .like(OperationLog::getContent, trimmedKeyword)
              .or()
              .like(OperationLog::getTargetId, trimmedKeyword);
          if (keywordOperatorId != null) {
            query.or().eq(OperationLog::getOperatorId, keywordOperatorId);
          }
        });
    wrapper.orderByDesc(OperationLog::getCreatedAt).orderByDesc(OperationLog::getId);
    return selectPage(page, wrapper);
  }

  private Integer parseInteger(String value) {
    if (value == null || !value.matches("\\d+")) {
      return null;
    }
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
