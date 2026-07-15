package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileComment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberProfileCommentMapper extends BaseMapper<MemberProfileComment> {
  default List<MemberProfileComment> selectVisibleByProfileUserId(Integer profileUserId) {
    return selectList(
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getProfileUserId, profileUserId)
            .eq(MemberProfileComment::getStatus, "visible")
            .orderByAsc(MemberProfileComment::getId));
  }

  default Long countVisibleByProfileUserId(Integer profileUserId) {
    return selectCount(
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getProfileUserId, profileUserId)
            .eq(MemberProfileComment::getStatus, "visible"));
  }
}
