package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.BlogArticleInteraction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客互动记录数据访问层
 *
 * <p>提供点赞、收藏、分享记录的查询和清理能力
 */
@Mapper
public interface BlogArticleInteractionMapper extends BaseMapper<BlogArticleInteraction> {

  default Page<BlogArticleInteraction> selectPageByConditions(
      Page<BlogArticleInteraction> page, String interactionType, Integer articleId) {
    LambdaQueryWrapper<BlogArticleInteraction> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .eq(articleId != null, BlogArticleInteraction::getArticleId, articleId)
        .eq(
            interactionType != null && !interactionType.isBlank(),
            BlogArticleInteraction::getInteractionType,
            interactionType)
        .orderByDesc(BlogArticleInteraction::getId);
    return selectPage(page, wrapper);
  }

  default BlogArticleInteraction selectLatestByArticleUserType(
      Integer articleId, Integer userId, String interactionType) {
    return selectOne(
        new LambdaQueryWrapper<BlogArticleInteraction>()
            .eq(BlogArticleInteraction::getArticleId, articleId)
            .eq(BlogArticleInteraction::getUserId, userId)
            .eq(BlogArticleInteraction::getInteractionType, interactionType)
            .orderByDesc(BlogArticleInteraction::getId)
            .last("LIMIT 1"));
  }

  default int deleteByArticleId(Integer articleId) {
    return delete(
        new LambdaQueryWrapper<BlogArticleInteraction>()
            .eq(BlogArticleInteraction::getArticleId, articleId));
  }
}
