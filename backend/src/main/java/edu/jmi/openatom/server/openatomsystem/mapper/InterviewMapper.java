package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 面试数据访问层
 *
 * <p>提供对面试(Interview)的数据库操作, 包括按申请ID列表查询面试以及按条件查询面试列表等功能
 */
@Mapper
public interface InterviewMapper extends BaseMapper<Interview> {

  /** 按申请ID列表查面试（ordered by round asc, id desc） */
  default List<Interview> selectByApplicationIds(List<Integer> applicationIds) {
    return selectList(
        new LambdaQueryWrapper<Interview>()
            .in(Interview::getApplicationId, applicationIds)
            .orderByAsc(Interview::getRound)
            .orderByDesc(Interview::getId));
  }

  /** 按条件查面试列表 */
  default List<Interview> selectByConditions(
      Integer applicationId, String status, List<Integer> applicationIds,
      List<Integer> interviewIds) {
    LambdaQueryWrapper<Interview> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(applicationId != null, Interview::getApplicationId, applicationId)
        .eq(status != null && !status.isBlank(), Interview::getStatus, status)
        .orderByDesc(Interview::getId);
    if (applicationIds != null) {
      wrapper.in(Interview::getApplicationId, applicationIds);
    }
    if (interviewIds != null) {
      wrapper.in(Interview::getId, interviewIds);
    }
    return selectList(wrapper);
  }
}
