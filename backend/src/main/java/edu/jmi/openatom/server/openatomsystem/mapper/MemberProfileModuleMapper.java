package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileModule;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberProfileModuleMapper extends BaseMapper<MemberProfileModule> {
  default List<MemberProfileModule> selectByProfileId(Long profileId) {
    return selectList(
        new LambdaQueryWrapper<MemberProfileModule>()
            .eq(MemberProfileModule::getProfileId, profileId)
            .orderByAsc(MemberProfileModule::getSortOrder)
            .orderByAsc(MemberProfileModule::getId));
  }

  default int deleteByProfileId(Long profileId) {
    return delete(
        new LambdaQueryWrapper<MemberProfileModule>()
            .eq(MemberProfileModule::getProfileId, profileId));
  }
}
