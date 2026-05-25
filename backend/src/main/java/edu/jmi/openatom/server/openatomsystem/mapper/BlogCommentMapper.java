package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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

  default List<BlogComment> selectVisibleByArticleId(Integer articleId) {
    return selectList(
        new LambdaQueryWrapper<BlogComment>()
            .eq(BlogComment::getArticleId, articleId)
            .eq(BlogComment::getStatus, "visible")
            .orderByAsc(BlogComment::getId));
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
            .eq(BlogComment::getStatus, "visible"));
  }

  default int deleteByArticleId(Integer articleId) {
    return delete(new LambdaQueryWrapper<BlogComment>().eq(BlogComment::getArticleId, articleId));
  }
}
