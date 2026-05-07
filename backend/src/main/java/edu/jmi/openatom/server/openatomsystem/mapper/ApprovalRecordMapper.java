package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ApprovalRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

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
