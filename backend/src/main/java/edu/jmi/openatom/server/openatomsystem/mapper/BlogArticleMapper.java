package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.entity.BlogArticle;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客文章数据访问层
 *
 * <p>提供文章分页、公开文章查询和浏览量递增等操作
 */
@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

  default Page<BlogArticle> selectPageByConditions(
      Page<BlogArticle> page,
      String status,
      String keyword,
      String category,
      String tag,
      Integer authorId,
      Boolean featured,
      boolean publicOnly) {
    LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
    if (publicOnly) {
      wrapper.eq(BlogArticle::getStatus, "published");
    } else {
      wrapper.eq(status != null && !status.isBlank(), BlogArticle::getStatus, status);
    }
    wrapper
        .eq(authorId != null, BlogArticle::getAuthorId, authorId)
        .eq(featured != null, BlogArticle::getFeatured, featured)
        .eq(category != null && !category.isBlank(), BlogArticle::getCategory, category)
        .like(tag != null && !tag.isBlank(), BlogArticle::getTags, tag == null ? null : tag.trim())
        .and(
            keyword != null && !keyword.isBlank(),
            query -> {
              String trimmed = keyword.trim();
              query
                  .like(BlogArticle::getTitle, trimmed)
                  .or()
                  .like(BlogArticle::getSummary, trimmed)
                  .or()
                  .like(BlogArticle::getContentMarkdown, trimmed)
                  .or()
                  .like(BlogArticle::getCategory, trimmed)
                  .or()
                  .like(BlogArticle::getTags, trimmed);
            });
    if (publicOnly) {
      wrapper
          .orderByDesc(BlogArticle::getFeatured)
          .orderByDesc(BlogArticle::getPublishedAt)
          .orderByDesc(BlogArticle::getId);
    } else {
      wrapper.orderByDesc(BlogArticle::getUpdatedAt).orderByDesc(BlogArticle::getId);
    }
    return selectPage(page, wrapper);
  }

  default BlogArticle selectPublishedById(Integer articleId) {
    return selectOne(
        new LambdaQueryWrapper<BlogArticle>()
            .eq(BlogArticle::getId, articleId)
            .eq(BlogArticle::getStatus, "published")
            .last("LIMIT 1"));
  }

  default Long countPublishedByAuthor(Integer authorId) {
    return selectCount(
        new LambdaQueryWrapper<BlogArticle>()
            .eq(BlogArticle::getAuthorId, authorId)
            .eq(BlogArticle::getStatus, "published"));
  }

  default List<BlogArticle> selectPublishedWithCategory() {
    return selectList(
        new LambdaQueryWrapper<BlogArticle>()
            .select(BlogArticle::getCategory)
            .eq(BlogArticle::getStatus, "published")
            .isNotNull(BlogArticle::getCategory)
            .ne(BlogArticle::getCategory, ""));
  }

  default int incrementViewCount(Integer articleId) {
    return update(
        null,
        new LambdaUpdateWrapper<BlogArticle>()
            .eq(BlogArticle::getId, articleId)
            .setSql("view_count = COALESCE(view_count, 0) + 1"));
  }

  default int incrementLikeCount(Integer articleId) {
    return update(
        null,
        new LambdaUpdateWrapper<BlogArticle>()
            .eq(BlogArticle::getId, articleId)
            .setSql("like_count = COALESCE(like_count, 0) + 1"));
  }

  default int incrementFavoriteCount(Integer articleId) {
    return update(
        null,
        new LambdaUpdateWrapper<BlogArticle>()
            .eq(BlogArticle::getId, articleId)
            .setSql("favorite_count = COALESCE(favorite_count, 0) + 1"));
  }

  default int incrementShareCount(Integer articleId) {
    return update(
        null,
        new LambdaUpdateWrapper<BlogArticle>()
            .eq(BlogArticle::getId, articleId)
            .setSql("share_count = COALESCE(share_count, 0) + 1"));
  }
}
