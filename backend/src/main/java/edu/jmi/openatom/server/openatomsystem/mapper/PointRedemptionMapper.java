package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.PointRedemption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointRedemptionMapper extends BaseMapper<PointRedemption> {
  default List<PointRedemption> selectByStatus(String status) {
    LambdaQueryWrapper<PointRedemption> wrapper =
        new LambdaQueryWrapper<PointRedemption>().orderByDesc(PointRedemption::getCreatedAt);
    if (status != null && !status.isBlank()) wrapper.eq(PointRedemption::getStatus, status.trim());
    return selectList(wrapper.last("LIMIT 300"));
  }

  default List<PointRedemption> selectByUserId(Integer userId) {
    if (userId == null) return List.of();
    return selectList(
        new LambdaQueryWrapper<PointRedemption>()
            .eq(PointRedemption::getUserId, userId)
            .orderByDesc(PointRedemption::getCreatedAt)
            .last("LIMIT 100"));
  }
}
