package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.SiteForm;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站点表单数据访问层
 *
 * <p>提供对站点表单(SiteForm)的数据库操作, 包括查询开放或已发布的表单以及按社团ID查询表单列表等功能
 */
@Mapper
public interface SiteFormMapper extends BaseMapper<SiteForm> {

  /** 查 open/published 的表单 */
  default List<SiteForm> selectOpenByClubId(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<SiteForm>()
            .eq(SiteForm::getClubId, clubId)
            .in(SiteForm::getStatus, List.of("open", "published"))
            .orderByDesc(SiteForm::getStartAt)
            .orderByDesc(SiteForm::getId));
  }

  /** 按社团查表单（ordered by id desc） */
  default List<SiteForm> selectByClubIdOrdered(Integer clubId) {
    return selectList(
        new LambdaQueryWrapper<SiteForm>()
            .eq(SiteForm::getClubId, clubId)
            .orderByDesc(SiteForm::getId));
  }
}