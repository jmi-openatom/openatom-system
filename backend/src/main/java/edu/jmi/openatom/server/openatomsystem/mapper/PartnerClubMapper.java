package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.PartnerClub;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 开源伙伴公开查询。 */
@Mapper
public interface PartnerClubMapper extends BaseMapper<PartnerClub> {
  default Page<PartnerClub> selectPageByConditions(
      Page<PartnerClub> page, String keyword, String status, Boolean featured) {
    LambdaQueryWrapper<PartnerClub> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(status != null && !status.isBlank(), PartnerClub::getStatus, status)
        .eq(featured != null, PartnerClub::getFeatured, featured)
        .and(
            keyword != null && !keyword.isBlank(),
            query ->
                query
                    .like(PartnerClub::getName, keyword)
                    .or()
                    .like(PartnerClub::getOrganization, keyword)
                    .or()
                    .like(PartnerClub::getCategory, keyword));
    wrapper.orderByAsc(PartnerClub::getSortOrder).orderByDesc(PartnerClub::getCreatedAt);
    return selectPage(page, wrapper);
  }

  default List<PartnerClub> selectPublished(Boolean featured, Integer limit) {
    LambdaQueryWrapper<PartnerClub> wrapper =
        new LambdaQueryWrapper<PartnerClub>()
            .eq(PartnerClub::getStatus, "published")
            .eq(Boolean.TRUE.equals(featured), PartnerClub::getFeatured, true)
            .orderByAsc(PartnerClub::getSortOrder)
            .orderByDesc(PartnerClub::getCreatedAt)
            .orderByDesc(PartnerClub::getId);
    if (limit != null) {
      wrapper.last("LIMIT " + Math.max(1, Math.min(limit, 100)));
    }
    return selectList(wrapper);
  }
}
