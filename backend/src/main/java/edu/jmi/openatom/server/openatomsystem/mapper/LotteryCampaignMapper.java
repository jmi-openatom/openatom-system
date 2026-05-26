package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.LotteryCampaign;
import org.apache.ibatis.annotations.Mapper;

/** 抽奖活动数据访问层 */
@Mapper
public interface LotteryCampaignMapper extends BaseMapper<LotteryCampaign> {}
