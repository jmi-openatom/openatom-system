package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CommentReport;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentReportMapper extends BaseMapper<CommentReport> {
  default CommentReport selectOne(String targetType, Long commentId, Integer userId) {
    return selectOne(new LambdaQueryWrapper<CommentReport>()
        .eq(CommentReport::getTargetType, targetType)
        .eq(CommentReport::getCommentId, commentId)
        .eq(CommentReport::getReporterUserId, userId));
  }

  default List<CommentReport> selectPendingByComments(String targetType, Collection<Long> commentIds) {
    if (commentIds == null || commentIds.isEmpty()) return List.of();
    return selectList(new LambdaQueryWrapper<CommentReport>()
        .eq(CommentReport::getTargetType, targetType)
        .eq(CommentReport::getStatus, "pending")
        .in(CommentReport::getCommentId, commentIds));
  }

  default int resolveByComment(String targetType, Long commentId) {
    return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CommentReport>()
        .eq(CommentReport::getTargetType, targetType)
        .eq(CommentReport::getCommentId, commentId)
        .eq(CommentReport::getStatus, "pending")
        .set(CommentReport::getStatus, "resolved"));
  }
}
