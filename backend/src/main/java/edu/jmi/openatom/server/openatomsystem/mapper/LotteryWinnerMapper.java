package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LotteryWinner;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 抽奖中奖记录数据访问层 */
@Mapper
public interface LotteryWinnerMapper extends BaseMapper<LotteryWinner> {
  default List<LotteryWinner> selectByLotteryId(Integer lotteryId) {
    return selectList(
        new LambdaQueryWrapper<LotteryWinner>()
            .eq(LotteryWinner::getLotteryId, lotteryId)
            .orderByDesc(LotteryWinner::getWonAt)
            .orderByDesc(LotteryWinner::getId));
  }

  default Long countByLotteryId(Integer lotteryId) {
    return selectCount(new LambdaQueryWrapper<LotteryWinner>().eq(LotteryWinner::getLotteryId, lotteryId));
  }

  default Long countByPrizeId(Integer prizeId) {
    return selectCount(new LambdaQueryWrapper<LotteryWinner>().eq(LotteryWinner::getPrizeId, prizeId));
  }

  default void deleteByLotteryId(Integer lotteryId) {
    delete(new LambdaUpdateWrapper<LotteryWinner>().eq(LotteryWinner::getLotteryId, lotteryId));
  }
}
