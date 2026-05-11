package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.OperationLog;
import java.sql.Timestamp;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志数据访问层
 *
 * <p>提供对操作日志(OperationLog)的数据库操作, 包括按操作人, 模块, 动作和时间范围等条件查询日志等功能
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

  /** 按条件查操作日志 */
  default List<OperationLog> selectByConditions(
      Integer operatorId, String module, String action,
      Timestamp startTime, Timestamp endTime) {
    LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(operatorId != null, OperationLog::getOperatorId, operatorId);
    wrapper.eq(module != null && !module.isBlank(), OperationLog::getModule, module);
    wrapper.eq(action != null && !action.isBlank(), OperationLog::getAction, action);
    wrapper.ge(startTime != null, OperationLog::getCreatedAt, startTime);
    wrapper.le(endTime != null, OperationLog::getCreatedAt, endTime);
    wrapper.orderByDesc(OperationLog::getCreatedAt);
    return selectList(wrapper);
  }
}
