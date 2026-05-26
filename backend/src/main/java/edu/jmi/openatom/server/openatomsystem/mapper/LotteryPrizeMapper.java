package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LotteryPrize;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 抽奖奖品数据访问层 */
@Mapper
public interface LotteryPrizeMapper extends BaseMapper<LotteryPrize> {
  default List<LotteryPrize> selectByLotteryId(Integer lotteryId) {
    return selectList(
        new LambdaQueryWrapper<LotteryPrize>()
            .eq(LotteryPrize::getLotteryId, lotteryId)
            .orderByAsc(LotteryPrize::getSortOrder)
            .orderByAsc(LotteryPrize::getId));
  }
}
