package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.PointAccount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PointAccountMapper extends BaseMapper<PointAccount> {
  default PointAccount selectOneByUserId(Integer userId) {
    if (userId == null) return null;
    return selectOne(
        new LambdaQueryWrapper<PointAccount>()
            .eq(PointAccount::getUserId, userId)
            .last("LIMIT 1"));
  }

  default List<PointAccount> selectByUserIds(List<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) return List.of();
    return selectList(
        new LambdaQueryWrapper<PointAccount>()
            .in(PointAccount::getUserId, userIds)
            .orderByDesc(PointAccount::getBalance)
            .orderByDesc(PointAccount::getTotalEarned)
            .orderByAsc(PointAccount::getUserId));
  }

  default List<PointAccount> selectOrdered(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return selectList(
        new LambdaQueryWrapper<PointAccount>()
            .orderByDesc(PointAccount::getBalance)
            .orderByDesc(PointAccount::getTotalEarned)
            .orderByAsc(PointAccount::getUserId)
            .last("LIMIT " + safeLimit));
  }

  default List<PointAccount> selectAllOrdered() {
    return selectList(
        new LambdaQueryWrapper<PointAccount>()
            .orderByDesc(PointAccount::getBalance)
            .orderByDesc(PointAccount::getTotalEarned)
            .orderByAsc(PointAccount::getUserId));
  }
}
