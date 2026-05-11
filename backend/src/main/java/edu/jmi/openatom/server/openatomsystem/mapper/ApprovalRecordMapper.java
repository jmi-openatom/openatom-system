package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录数据访问层
 *
 * <p>提供对审批记录(ApprovalRecord)的数据库操作, 包括按申请ID查询审批记录等功能
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

  /** 按申请ID查审批记录（ordered by id asc） */
  default List<ApprovalRecord> selectByApplicationIdOrdered(Integer applicationId) {
    return selectList(
        new LambdaQueryWrapper<ApprovalRecord>()
            .eq(ApprovalRecord::getApplicationId, applicationId)
            .orderByAsc(ApprovalRecord::getId));
  }
}
