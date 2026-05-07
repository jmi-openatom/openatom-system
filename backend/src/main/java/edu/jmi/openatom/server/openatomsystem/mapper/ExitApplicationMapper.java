package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExitApplicationMapper extends BaseMapper<ExitApplication> {

  /** 查所有退社申请（ordered by id desc） */
  default List<ExitApplication> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<ExitApplication>().orderByDesc(ExitApplication::getId));
  }
}
