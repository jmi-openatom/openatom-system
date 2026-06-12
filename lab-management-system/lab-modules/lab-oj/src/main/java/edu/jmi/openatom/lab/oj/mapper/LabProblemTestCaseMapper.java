package edu.jmi.openatom.lab.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.oj.entity.LabProblemTestCase;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabProblemTestCaseMapper extends BaseMapper<LabProblemTestCase> {
  default List<LabProblemTestCase> selectByProblemId(Long problemId) {
    return selectList(
        new LambdaQueryWrapper<LabProblemTestCase>()
            .eq(LabProblemTestCase::getProblemId, problemId)
            .orderByAsc(LabProblemTestCase::getSortOrder)
            .orderByAsc(LabProblemTestCase::getId));
  }

  default int deleteByProblemId(Long problemId) {
    return delete(new LambdaQueryWrapper<LabProblemTestCase>().eq(LabProblemTestCase::getProblemId, problemId));
  }
}
