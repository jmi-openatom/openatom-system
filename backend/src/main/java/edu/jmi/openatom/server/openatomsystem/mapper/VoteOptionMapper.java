package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.VoteOption;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 投票选项数据访问层 */
@Mapper
public interface VoteOptionMapper extends BaseMapper<VoteOption> {
  default List<VoteOption> selectByVoteId(Integer voteId) {
    return selectList(
        new LambdaQueryWrapper<VoteOption>()
            .eq(VoteOption::getVoteId, voteId)
            .orderByAsc(VoteOption::getSortOrder)
            .orderByAsc(VoteOption::getId));
  }
}
