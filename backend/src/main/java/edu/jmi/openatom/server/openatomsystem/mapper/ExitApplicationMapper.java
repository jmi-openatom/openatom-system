package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ExitApplication;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退社申请数据访问层
 *
 * <p>提供对退社申请(ExitApplication)的数据库操作, 包括查询所有退社申请并按ID降序排序等功能
 */
@Mapper
public interface ExitApplicationMapper extends BaseMapper<ExitApplication> {

  /** 查所有退社申请（ordered by id desc） */
  default List<ExitApplication> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<ExitApplication>().orderByDesc(ExitApplication::getId));
  }
}
