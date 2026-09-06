package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CommentInteraction;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentInteractionMapper extends BaseMapper<CommentInteraction> {
  default CommentInteraction selectOne(String targetType, Long commentId, Integer userId) {
    return selectOne(new LambdaQueryWrapper<CommentInteraction>()
        .eq(CommentInteraction::getTargetType, targetType)
        .eq(CommentInteraction::getCommentId, commentId)
        .eq(CommentInteraction::getUserId, userId));
  }

  default List<CommentInteraction> selectByComments(String targetType, Collection<Long> commentIds) {
    if (commentIds == null || commentIds.isEmpty()) return List.of();
    return selectList(new LambdaQueryWrapper<CommentInteraction>()
        .eq(CommentInteraction::getTargetType, targetType)
        .in(CommentInteraction::getCommentId, commentIds));
  }
}
