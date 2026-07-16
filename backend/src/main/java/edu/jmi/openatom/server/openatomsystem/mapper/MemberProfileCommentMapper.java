package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

  default Page<MemberProfileComment> selectAdminPage(
      Page<MemberProfileComment> page, String keyword, String status) {
    LambdaQueryWrapper<MemberProfileComment> wrapper =
        new LambdaQueryWrapper<MemberProfileComment>().orderByDesc(MemberProfileComment::getId);
    if (keyword != null && !keyword.isBlank()) {
      wrapper.like(MemberProfileComment::getContent, keyword.trim());
    }
    if (status != null && !status.isBlank()) {
      wrapper.eq(MemberProfileComment::getStatus, status.trim());
    }
    return selectPage(page, wrapper);
  }
}
