package edu.jmi.openatom.lab.oj.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.oj.entity.LabProblem;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabProblemMapper extends BaseMapper<LabProblem> {
  default LabProblem selectByDate(LocalDate date) {
    return selectOne(
        new LambdaQueryWrapper<LabProblem>()
            .eq(LabProblem::getProblemDate, date)
            .last("LIMIT 1"));
  }

  default List<LabProblem> selectRecent(int limit) {
    return selectList(
        new LambdaQueryWrapper<LabProblem>()
            .orderByDesc(LabProblem::getProblemDate)
            .last("LIMIT " + Math.max(1, limit)));
  }
}
