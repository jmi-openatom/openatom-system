package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.PointTransaction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointTransactionMapper extends BaseMapper<PointTransaction> {
  @Select(
      """
      SELECT COALESCE(SUM(delta), 0)
      FROM point_transaction
      WHERE user_id = #{userId} AND source_key = #{sourceKey}
      """)
  Long sumDeltaBySourceKey(@Param("userId") Integer userId, @Param("sourceKey") String sourceKey);

  default List<PointTransaction> selectRecent(Integer userId, String type, int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 300));
    LambdaQueryWrapper<PointTransaction> wrapper =
        new LambdaQueryWrapper<PointTransaction>().orderByDesc(PointTransaction::getCreatedAt);
    if (userId != null) wrapper.eq(PointTransaction::getUserId, userId);
    if (type != null && !type.isBlank()) wrapper.eq(PointTransaction::getType, type.trim());
    return selectList(wrapper.last("LIMIT " + safeLimit));
  }
}
