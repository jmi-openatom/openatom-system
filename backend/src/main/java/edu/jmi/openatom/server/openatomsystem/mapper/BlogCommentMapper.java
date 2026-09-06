package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.BlogComment;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客评论数据访问层
 *
 * <p>提供文章评论查询、计数和按文章清理等操作
 */
@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

  default Page<BlogComment> selectVisibleRootPage(
      Page<BlogComment> page, Integer articleId, String sort) {
    LambdaQueryWrapper<BlogComment> wrapper =
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getArticleId, articleId)
            .eq(BlogComment::getStatus, "visible")
            .isNull(BlogComment::getParentId);
    if ("oldest".equals(sort)) wrapper.orderByAsc(BlogComment::getId);
    else wrapper.orderByDesc(BlogComment::getId);
    return selectPage(page, wrapper);
  }

  default List<BlogComment> selectVisibleReplyPreview(Integer rootId, int limit) {
    return selectList(
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getRootId, rootId)
            .eq(BlogComment::getStatus, "visible")
            .orderByAsc(BlogComment::getId)
            .last("LIMIT " + Math.max(1, Math.min(limit, 20))));
  }

  default Long countVisibleReplies(Integer rootId) {
    return selectCount(
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getRootId, rootId)
            .eq(BlogComment::getStatus, "visible"));
  }

  default Page<BlogComment> selectVisibleReplyPage(
      Page<BlogComment> page, Integer articleId, Integer rootId) {
    return selectPage(
        page,
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getArticleId, articleId)
            .eq(BlogComment::getRootId, rootId)
            .eq(BlogComment::getStatus, "visible")
            .orderByAsc(BlogComment::getId));
  }

  default int updateLikeCount(Integer commentId, int delta) {
    return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<BlogComment>()
        .eq(BlogComment::getId, commentId)
        .setSql("like_count = GREATEST(0, like_count + " + delta + ")"));
  }

  default List<BlogComment> selectByArticleIdOrdered(Integer articleId) {
    return selectList(
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getArticleId, articleId)
            .orderByDesc(BlogComment::getId));
  }

  default Long countVisibleByArticleId(Integer articleId) {
    return selectCount(
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getArticleId, articleId)
            .eq(BlogComment::getStatus, "visible")
            .and(query -> query.isNull(BlogComment::getRootId)
                .or().inSql(BlogComment::getRootId,
                    "SELECT id FROM blog_comment WHERE article_id = " + articleId
                        + " AND parent_id IS NULL AND status = 'visible'")));
  }

  default int deleteByArticleId(Integer articleId) {
    return delete(new LambdaQueryWrapper<BlogComment>().eq(BlogComment::getArticleId, articleId));
  }
}
