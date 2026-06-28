package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAlumniGroup;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 往届管理人员分组数据访问层
 */
@Mapper
public interface ClubAlumniGroupMapper extends BaseMapper<ClubAlumniGroup> {

  /** 按 clubId 查分组列表（按 sortOrder 排序） */
  default List<ClubAlumniGroup> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<ClubAlumniGroup>()
            .eq(ClubAlumniGroup::getClubId, clubId)
            .orderByAsc(ClubAlumniGroup::getSortOrder)
            .orderByAsc(ClubAlumniGroup::getId));
  }
}
