package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.PointRedeemItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointRedeemItemMapper extends BaseMapper<PointRedeemItem> {
  default List<PointRedeemItem> selectAdmin(Boolean includeInactive) {
    LambdaQueryWrapper<PointRedeemItem> wrapper =
        new LambdaQueryWrapper<PointRedeemItem>()
            .orderByAsc(PointRedeemItem::getSortOrder)
            .orderByDesc(PointRedeemItem::getId);
    if (!Boolean.TRUE.equals(includeInactive)) wrapper.eq(PointRedeemItem::getStatus, "active");
    return selectList(wrapper);
  }

  default List<PointRedeemItem> selectActive() {
    return selectList(
        new LambdaQueryWrapper<PointRedeemItem>()
            .eq(PointRedeemItem::getStatus, "active")
            .orderByAsc(PointRedeemItem::getSortOrder)
            .orderByDesc(PointRedeemItem::getId));
  }
}
