package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.MemberProfileComment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberProfileCommentMapper extends BaseMapper<MemberProfileComment> {
  default Page<MemberProfileComment> selectVisibleRootPage(
      Page<MemberProfileComment> page, Integer profileUserId, String sort) {
    LambdaQueryWrapper<MemberProfileComment> wrapper =
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getProfileUserId, profileUserId)
            .eq(MemberProfileComment::getStatus, "visible")
            .isNull(MemberProfileComment::getParentId);
    if ("oldest".equals(sort)) wrapper.orderByAsc(MemberProfileComment::getId);
    else wrapper.orderByDesc(MemberProfileComment::getId);
    return selectPage(page, wrapper);
  }

  default List<MemberProfileComment> selectVisibleReplyPreview(Long rootId, int limit) {
    return selectList(
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getRootId, rootId)
            .eq(MemberProfileComment::getStatus, "visible")
            .orderByAsc(MemberProfileComment::getId)
            .last("LIMIT " + Math.max(1, Math.min(limit, 20))));
  }

  default Long countVisibleReplies(Long rootId) {
    return selectCount(
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getRootId, rootId)
            .eq(MemberProfileComment::getStatus, "visible"));
  }

  default Page<MemberProfileComment> selectVisibleReplyPage(
      Page<MemberProfileComment> page, Integer profileUserId, Long rootId) {
    return selectPage(
        page,
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getProfileUserId, profileUserId)
            .eq(MemberProfileComment::getRootId, rootId)
            .eq(MemberProfileComment::getStatus, "visible")
            .orderByAsc(MemberProfileComment::getId));
  }

  default int updateLikeCount(Long commentId, int delta) {
    return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MemberProfileComment>()
        .eq(MemberProfileComment::getId, commentId)
        .setSql("like_count = GREATEST(0, like_count + " + delta + ")"));
  }

  default Long countVisibleByProfileUserId(Integer profileUserId) {
    return selectCount(
        new LambdaQueryWrapper<MemberProfileComment>()
            .eq(MemberProfileComment::getProfileUserId, profileUserId)
            .eq(MemberProfileComment::getStatus, "visible")
            .and(query -> query.isNull(MemberProfileComment::getRootId)
                .or().inSql(MemberProfileComment::getRootId,
                    "SELECT id FROM member_profile_comment WHERE profile_user_id = " + profileUserId
                        + " AND parent_id IS NULL AND status = 'visible'")));
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
