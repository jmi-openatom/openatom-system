package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.VoteCampaign;
import org.apache.ibatis.annotations.Mapper;

/** 投票活动数据访问层 */
@Mapper
public interface VoteCampaignMapper extends BaseMapper<VoteCampaign> {}
