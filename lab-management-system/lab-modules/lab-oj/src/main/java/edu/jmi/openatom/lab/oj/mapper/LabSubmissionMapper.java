package edu.jmi.openatom.lab.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.oj.entity.LabSubmission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabSubmissionMapper extends BaseMapper<LabSubmission> {
  default List<LabSubmission> selectByUser(Long userId, int limit) {
    return selectList(
        new LambdaQueryWrapper<LabSubmission>()
            .eq(LabSubmission::getUserId, userId)
            .orderByDesc(LabSubmission::getSubmittedAt)
            .last("LIMIT " + Math.max(1, limit)));
  }

  default long countAccepted(Long userId) {
    return selectCount(
        new LambdaQueryWrapper<LabSubmission>()
            .eq(LabSubmission::getUserId, userId)
            .eq(LabSubmission::getJudgeStatus, "AC"));
  }

  default int countPending() {
    return Math.toIntExact(
        selectCount(new LambdaQueryWrapper<LabSubmission>().eq(LabSubmission::getJudgeStatus, "PENDING")));
  }
}
