package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import edu.jmi.openatom.server.openatomsystem.entity.VoteRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/** 投票记录数据访问层 */
@Mapper
public interface VoteRecordMapper extends BaseMapper<VoteRecord> {
  default List<VoteRecord> selectByVoteId(Integer voteId) {
    return selectList(
        new LambdaQueryWrapper<VoteRecord>()
            .eq(VoteRecord::getVoteId, voteId)
            .orderByDesc(VoteRecord::getId));
  }

  default Long countByVoteId(Integer voteId) {
    return selectCount(new LambdaQueryWrapper<VoteRecord>().eq(VoteRecord::getVoteId, voteId));
  }

  default Long countByVoterKey(Integer voteId, String voterKey) {
    return selectCount(
        new LambdaQueryWrapper<VoteRecord>()
            .eq(VoteRecord::getVoteId, voteId)
            .eq(VoteRecord::getVoterKey, voterKey));
  }

  default void deleteByVoteId(Integer voteId) {
    delete(new LambdaUpdateWrapper<VoteRecord>().eq(VoteRecord::getVoteId, voteId));
  }
}
