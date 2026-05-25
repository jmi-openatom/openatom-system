package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBlogInteractionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateBlogCommentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.entity.BlogArticle;
import edu.jmi.openatom.server.openatomsystem.entity.BlogArticleInteraction;
import edu.jmi.openatom.server.openatomsystem.entity.BlogComment;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.BlogArticleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.BlogArticleInteractionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.BlogCommentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMembershipMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.BlogService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogArticleVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogCommentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogInteractionVO;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 博客服务实现
 *
 * <p>负责公开浏览、正式成员发布和管理员管控的完整博客业务链路
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";
  private static final List<String> AUTHOR_STATUSES = List.of("draft", "published");
  private static final List<String> ADMIN_STATUSES =
      List.of("draft", "published", "hidden", "rejected");
  private static final List<String> COMMENT_STATUSES = List.of("visible", "hidden");
  private static final List<String> INTERACTION_TYPES = List.of("like", "favorite", "share");

  private final BlogArticleMapper blogArticleMapper;
  private final BlogArticleInteractionMapper blogArticleInteractionMapper;
  private final BlogCommentMapper blogCommentMapper;
  private final ClubMapper clubMapper;
  private final ClubMembershipMapper clubMembershipMapper;
  private final UserMapper userMapper;

  @Override
  public Result<PageDataVO<ResponseBlogArticleVO>> publicArticles(
      String keyword, String category, String tag, Long page, Long pageSize) {
    Page<BlogArticle> articlePage =
        blogArticleMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            null,
            keyword,
            category,
            tag,
            null,
            true);
    return Result.success(toPage(articlePage));
  }

  @Override
  public Result<ResponseBlogArticleVO> publicDetail(Integer articleId) {
    BlogArticle article = blogArticleMapper.selectPublishedById(articleId);
    if (article == null) return Result.error(404, "文章不存在或未发布");
    blogArticleMapper.incrementViewCount(articleId);
    article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
    return Result.success(toResponse(article, userMap(List.of(article)), commentCount(article.getId())));
  }

  @Override
  public Result<List<String>> publicCategories() {
    List<String> categories =
        blogArticleMapper.selectPublishedWithCategory().stream()
            .map(BlogArticle::getCategory)
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    return Result.success(categories);
  }

  @Override
  public Result<List<ResponseBlogCommentVO>> publicComments(Integer articleId) {
    if (blogArticleMapper.selectPublishedById(articleId) == null) {
      return Result.error(404, "文章不存在或未发布");
    }
    return Result.success(toCommentTree(blogCommentMapper.selectVisibleByArticleId(articleId)));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> createComment(Integer articleId, RequestCreateBlogCommentDTO request) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后再评论");
    if (blogArticleMapper.selectPublishedById(articleId) == null) {
      return Result.error(404, "文章不存在或未发布");
    }
    String content = trimToNull(request == null ? null : request.getContent());
    if (content == null) return Result.error(400, "评论内容不能为空");
    if (content.length() > 1000) return Result.error(400, "评论内容不能超过1000字");
    Integer parentId = request == null ? null : request.getParentId();
    if (parentId != null) {
      BlogComment parent = blogCommentMapper.selectById(parentId);
      if (parent == null
          || !Objects.equals(parent.getArticleId(), articleId)
          || !"visible".equals(parent.getStatus())) {
        return Result.error(404, "要回复的评论不存在或不可见");
      }
    }
    BlogComment comment =
        BlogComment.builder()
            .articleId(articleId)
            .userId(StpUtil.getLoginIdAsInt())
            .parentId(parentId)
            .content(content)
            .status("visible")
            .build();
    int row = blogCommentMapper.insert(comment);
    return row > 0 ? Result.success("评论已发布") : Result.error("评论发布失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseBlogArticleVO> likeArticle(
      Integer articleId, RequestBlogInteractionDTO request) {
    return recordInteraction(articleId, "like", request, false);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseBlogArticleVO> favoriteArticle(
      Integer articleId, RequestBlogInteractionDTO request) {
    return recordInteraction(articleId, "favorite", request, false);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseBlogArticleVO> shareArticle(
      Integer articleId, RequestBlogInteractionDTO request) {
    return recordInteraction(articleId, "share", request, true);
  }

  @Override
  public Result<PageDataVO<ResponseBlogArticleVO>> myArticles(
      String status, Long page, Long pageSize) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    Page<BlogArticle> articlePage =
        blogArticleMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            status,
            null,
            null,
            null,
            StpUtil.getLoginIdAsInt(),
            false);
    return Result.success(toPage(articlePage));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseBlogArticleVO> createArticle(RequestCreateBlogArticleDTO request) {
    if (request == null) return Result.error(400, "文章内容不能为空");
    Result<Club> clubResult = requireAuthorClub();
    if (clubResult.getCode() != Result.SUCCESS_CODE) {
      return Result.error(clubResult.getCode(), clubResult.getMessage());
    }
    String title = trimToNull(request == null ? null : request.getTitle());
    String content = trimToNull(request == null ? null : request.getContentMarkdown());
    if (title == null) return Result.error(400, "文章标题不能为空");
    if (content == null) return Result.error(400, "文章正文不能为空");
    String status = normalizeAuthorStatus(request.getStatus());
    if (status == null) return Result.error(400, "文章状态不合法");
    Timestamp now = Times.now();
    BlogArticle article =
        BlogArticle.builder()
            .clubId(clubResult.getData().getId())
            .authorId(StpUtil.getLoginIdAsInt())
            .title(title)
            .summary(normalizeSummary(request.getSummary(), content))
            .contentMarkdown(content)
            .coverUrl(trimToNull(request.getCoverUrl()))
            .category(trimToNull(request.getCategory()))
            .tags(Jsons.stringify(normalizeTags(request.getTags())))
            .status(status)
            .featured(false)
            .viewCount(0)
            .likeCount(0)
            .favoriteCount(0)
            .shareCount(0)
            .publishedAt("published".equals(status) ? now : null)
            .build();
    int row = blogArticleMapper.insert(article);
    if (row <= 0) return Result.error("文章保存失败");
    return Result.success(toResponse(article, userMap(List.of(article)), 0L), "文章保存成功");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> updateOwnArticle(Integer articleId, RequestUpdateBlogArticleDTO request) {
    if (request == null) return Result.error(400, "文章内容不能为空");
    BlogArticle article = findOwnArticle(articleId);
    if (article == null) return Result.error(404, "文章不存在");
    Result<Club> clubResult = requireAuthorClub();
    if (clubResult.getCode() != Result.SUCCESS_CODE) {
      return Result.error(clubResult.getCode(), clubResult.getMessage());
    }
    if (request.getTitle() != null) {
      String title = trimToNull(request.getTitle());
      if (title == null) return Result.error(400, "文章标题不能为空");
      article.setTitle(title);
    }
    if (request.getContentMarkdown() != null) {
      String content = trimToNull(request.getContentMarkdown());
      if (content == null) return Result.error(400, "文章正文不能为空");
      article.setContentMarkdown(content);
      if (request.getSummary() == null) {
        article.setSummary(normalizeSummary(article.getSummary(), content));
      }
    }
    if (request.getSummary() != null) {
      article.setSummary(normalizeSummary(request.getSummary(), article.getContentMarkdown()));
    }
    if (request.getCoverUrl() != null) article.setCoverUrl(trimToNull(request.getCoverUrl()));
    if (request.getCategory() != null) article.setCategory(trimToNull(request.getCategory()));
    if (request.getTags() != null) article.setTags(Jsons.stringify(normalizeTags(request.getTags())));
    if (request.getStatus() != null) {
      if ("hidden".equals(article.getStatus())) {
        return Result.error(403, "文章已被管理员隐藏，不能自行发布");
      }
      String status = normalizeAuthorStatus(request.getStatus());
      if (status == null) return Result.error(400, "文章状态不合法");
      article.setStatus(status);
      if ("published".equals(status) && article.getPublishedAt() == null) {
        article.setPublishedAt(Times.now());
      }
      if ("published".equals(status)) {
        article.setRejectReason(null);
      }
    }
    int row = blogArticleMapper.updateById(article);
    return row > 0 ? Result.success("文章已保存") : Result.error("文章保存失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> publishOwnArticle(Integer articleId) {
    BlogArticle article = findOwnArticle(articleId);
    if (article == null) return Result.error(404, "文章不存在");
    Result<Club> clubResult = requireAuthorClub();
    if (clubResult.getCode() != Result.SUCCESS_CODE) {
      return Result.error(clubResult.getCode(), clubResult.getMessage());
    }
    if ("hidden".equals(article.getStatus())) {
      return Result.error(403, "文章已被管理员隐藏，不能自行发布");
    }
    article.setStatus("published");
    article.setRejectReason(null);
    if (article.getPublishedAt() == null) article.setPublishedAt(Times.now());
    int row = blogArticleMapper.updateById(article);
    return row > 0 ? Result.success("文章已发布") : Result.error("文章发布失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> deleteOwnArticle(Integer articleId) {
    BlogArticle article = findOwnArticle(articleId);
    if (article == null) return Result.error(404, "文章不存在");
    blogCommentMapper.deleteByArticleId(articleId);
    blogArticleInteractionMapper.deleteByArticleId(articleId);
    int row = blogArticleMapper.deleteById(articleId);
    return row > 0 ? Result.success("文章已删除") : Result.error("文章删除失败");
  }

  @Override
  public Result<PageDataVO<ResponseBlogArticleVO>> adminArticles(
      String status, String keyword, Long page, Long pageSize) {
    Page<BlogArticle> articlePage =
        blogArticleMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            status,
            keyword,
            null,
            null,
            null,
            false);
    return Result.success(toPage(articlePage));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> adminReviewArticle(Integer articleId, RequestReviewBlogArticleDTO request) {
    if (request == null) return Result.error(400, "审核内容不能为空");
    BlogArticle article = articleId == null ? null : blogArticleMapper.selectById(articleId);
    if (article == null) return Result.error(404, "文章不存在");
    if (request.getFeatured() != null) article.setFeatured(request.getFeatured());
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      String status = request.getStatus().trim();
      if (!ADMIN_STATUSES.contains(status)) return Result.error(400, "文章状态不合法");
      if ("rejected".equals(status) && trimToNull(request.getReason()) == null) {
        return Result.error(400, "驳回文章需要填写原因");
      }
      article.setStatus(status);
      article.setReviewedBy(StpUtil.getLoginIdAsInt());
      article.setReviewedAt(Times.now());
      article.setRejectReason(
          List.of("hidden", "rejected").contains(status) ? trimToNull(request.getReason()) : null);
      if ("published".equals(status) && article.getPublishedAt() == null) {
        article.setPublishedAt(Times.now());
      }
    }
    int row = blogArticleMapper.updateById(article);
    return row > 0 ? Result.success("文章状态已更新") : Result.error("文章状态更新失败");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> adminDeleteArticle(Integer articleId) {
    if (blogArticleMapper.selectById(articleId) == null) return Result.error(404, "文章不存在");
    blogCommentMapper.deleteByArticleId(articleId);
    blogArticleInteractionMapper.deleteByArticleId(articleId);
    int row = blogArticleMapper.deleteById(articleId);
    return row > 0 ? Result.success("文章已删除") : Result.error("文章删除失败");
  }

  @Override
  public Result<List<ResponseBlogCommentVO>> adminComments(Integer articleId) {
    if (blogArticleMapper.selectById(articleId) == null) return Result.error(404, "文章不存在");
    return Result.success(toCommentResponseList(blogCommentMapper.selectByArticleIdOrdered(articleId)));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<String> adminUpdateCommentStatus(Integer commentId, String status) {
    BlogComment comment = commentId == null ? null : blogCommentMapper.selectById(commentId);
    if (comment == null) return Result.error(404, "评论不存在");
    String normalized = trimToNull(status);
    if (!COMMENT_STATUSES.contains(normalized)) return Result.error(400, "评论状态不合法");
    comment.setStatus(normalized);
    int row = blogCommentMapper.updateById(comment);
    return row > 0 ? Result.success("评论状态已更新") : Result.error("评论状态更新失败");
  }

  @Override
  public Result<PageDataVO<ResponseBlogInteractionVO>> adminInteractions(
      String interactionType, Integer articleId, Long page, Long pageSize) {
    String normalized = trimToNull(interactionType);
    if (normalized != null && !INTERACTION_TYPES.contains(normalized)) {
      return Result.error(400, "互动类型不合法");
    }
    Page<BlogArticleInteraction> interactionPage =
        blogArticleInteractionMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            normalized,
            articleId);
    return Result.success(toInteractionPage(interactionPage));
  }

  private Result<ResponseBlogArticleVO> recordInteraction(
      Integer articleId,
      String interactionType,
      RequestBlogInteractionDTO request,
      boolean allowDuplicate) {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录后再操作");
    BlogArticle article = blogArticleMapper.selectPublishedById(articleId);
    if (article == null) return Result.error(404, "文章不存在或未发布");
    Integer userId = StpUtil.getLoginIdAsInt();
    if (!allowDuplicate
        && blogArticleInteractionMapper.selectLatestByArticleUserType(
                articleId, userId, interactionType)
            != null) {
      return Result.success(
          toResponse(article, userMap(List.of(article)), commentCount(articleId)), "互动已记录");
    }
    BlogArticleInteraction interaction =
        BlogArticleInteraction.builder()
            .articleId(articleId)
            .userId(userId)
            .interactionType(interactionType)
            .channel(normalizeInteractionChannel(request, interactionType))
            .createdAt(Times.now())
            .build();
    int row = blogArticleInteractionMapper.insert(interaction);
    if (row <= 0) return Result.error("互动记录保存失败");
    if ("like".equals(interactionType)) {
      blogArticleMapper.incrementLikeCount(articleId);
    } else if ("favorite".equals(interactionType)) {
      blogArticleMapper.incrementFavoriteCount(articleId);
    } else if ("share".equals(interactionType)) {
      blogArticleMapper.incrementShareCount(articleId);
    }
    BlogArticle updated = blogArticleMapper.selectPublishedById(articleId);
    BlogArticle responseArticle = updated == null ? article : updated;
    return Result.success(
        toResponse(responseArticle, userMap(List.of(responseArticle)), commentCount(articleId)),
        interactionMessage(interactionType));
  }

  private String normalizeInteractionChannel(
      RequestBlogInteractionDTO request, String interactionType) {
    String channel = trimToNull(request == null ? null : request.getChannel());
    if (channel == null) return "share".equals(interactionType) ? "copy_link" : "web";
    return channel.length() > 40 ? channel.substring(0, 40) : channel;
  }

  private String interactionMessage(String interactionType) {
    return switch (interactionType) {
      case "like" -> "点赞已记录";
      case "favorite" -> "收藏已记录";
      case "share" -> "分享已记录";
      default -> "互动已记录";
    };
  }

  private Result<Club> requireAuthorClub() {
    if (!StpUtil.isLogin()) return Result.error(401, "请先登录");
    Club club = defaultClub();
    if (club == null) return Result.error(404, "默认社团不存在");
    Integer userId = StpUtil.getLoginIdAsInt();
    Long membershipCount = clubMembershipMapper.countActiveMembership(userId, club.getId(), "active");
    if (membershipCount == null || membershipCount == 0) {
      return Result.error(403, "仅正式成员可以发布博客文章");
    }
    return Result.success(club);
  }

  private Club defaultClub() {
    return clubMapper.selectDefaultClub(DEFAULT_CLUB_CODE);
  }

  private BlogArticle findOwnArticle(Integer articleId) {
    if (!StpUtil.isLogin() || articleId == null) return null;
    BlogArticle article = blogArticleMapper.selectById(articleId);
    if (article == null || !Objects.equals(article.getAuthorId(), StpUtil.getLoginIdAsInt())) {
      return null;
    }
    return article;
  }

  private String normalizeAuthorStatus(String status) {
    String value = status == null || status.isBlank() ? "draft" : status.trim();
    return AUTHOR_STATUSES.contains(value) ? value : null;
  }

  private List<String> normalizeTags(List<String> tags) {
    if (tags == null) return List.of();
    return tags.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .limit(8)
        .distinct()
        .toList();
  }

  private String normalizeSummary(String summary, String content) {
    String value = trimToNull(summary);
    if (value == null) {
      value = trimToNull(stripMarkdown(content));
    }
    if (value == null) return null;
    return value.length() > 240 ? value.substring(0, 240) : value;
  }

  private String stripMarkdown(String content) {
    if (content == null) return null;
    return content
        .replaceAll("(?m)^#{1,6}\\s*", "")
        .replaceAll("[`*_>\\-\\[\\]()]"," ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private PageDataVO<ResponseBlogArticleVO> toPage(Page<BlogArticle> page) {
    return PageDataVO.<ResponseBlogArticleVO>builder()
        .list(toResponseList(page.getRecords()))
        .page(page.getCurrent())
        .pageSize(page.getSize())
        .total(page.getTotal())
        .build();
  }

  private PageDataVO<ResponseBlogInteractionVO> toInteractionPage(
      Page<BlogArticleInteraction> page) {
    return PageDataVO.<ResponseBlogInteractionVO>builder()
        .list(toInteractionResponseList(page.getRecords()))
        .page(page.getCurrent())
        .pageSize(page.getSize())
        .total(page.getTotal())
        .build();
  }

  private List<ResponseBlogArticleVO> toResponseList(List<BlogArticle> articles) {
    if (articles == null || articles.isEmpty()) return List.of();
    Map<Integer, User> users = userMap(articles);
    return articles.stream()
        .map(article -> toResponse(article, users, commentCount(article.getId())))
        .toList();
  }

  private Map<Integer, User> userMap(List<BlogArticle> articles) {
    List<Integer> userIds =
        articles.stream()
            .flatMap(article -> Stream.of(article.getAuthorId(), article.getReviewedBy()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (userIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
  }

  private Long commentCount(Integer articleId) {
    Long count = blogCommentMapper.countVisibleByArticleId(articleId);
    return count == null ? 0L : count;
  }

  private List<ResponseBlogInteractionVO> toInteractionResponseList(
      List<BlogArticleInteraction> interactions) {
    if (interactions == null || interactions.isEmpty()) return List.of();
    Map<Integer, BlogArticle> articles = articleMap(interactions);
    Map<Integer, User> users =
        userMapByIds(
            interactions.stream()
                .map(BlogArticleInteraction::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    return interactions.stream()
        .map(
            interaction -> {
              BlogArticle article = articles.get(interaction.getArticleId());
              User user = users.get(interaction.getUserId());
              return ResponseBlogInteractionVO.builder()
                  .id(interaction.getId())
                  .articleId(interaction.getArticleId())
                  .articleTitle(article == null ? null : article.getTitle())
                  .userId(interaction.getUserId())
                  .userName(displayName(user))
                  .userAvatar(user == null ? null : user.getAvatar())
                  .interactionType(interaction.getInteractionType())
                  .channel(interaction.getChannel())
                  .createdAt(interaction.getCreatedAt())
                  .build();
            })
        .toList();
  }

  private Map<Integer, BlogArticle> articleMap(List<BlogArticleInteraction> interactions) {
    List<Integer> articleIds =
        interactions.stream()
            .map(BlogArticleInteraction::getArticleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (articleIds.isEmpty()) return Map.of();
    return blogArticleMapper.selectBatchIds(articleIds).stream()
        .collect(Collectors.toMap(BlogArticle::getId, Function.identity(), (left, right) -> left));
  }

  private Map<Integer, User> userMapByIds(List<Integer> userIds) {
    if (userIds == null || userIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
  }

  private ResponseBlogArticleVO toResponse(
      BlogArticle article, Map<Integer, User> users, Long commentCount) {
    User author = article.getAuthorId() == null ? null : users.get(article.getAuthorId());
    User reviewer = article.getReviewedBy() == null ? null : users.get(article.getReviewedBy());
    Integer currentUserId = currentUserIdOrNull();
    return ResponseBlogArticleVO.builder()
        .id(article.getId())
        .clubId(article.getClubId())
        .authorId(article.getAuthorId())
        .authorName(displayName(author))
        .authorAvatar(author == null ? null : author.getAvatar())
        .title(article.getTitle())
        .summary(article.getSummary())
        .contentMarkdown(article.getContentMarkdown())
        .coverUrl(article.getCoverUrl())
        .category(article.getCategory())
        .tags(Jsons.parseStringList(article.getTags()))
        .status(article.getStatus())
        .featured(Boolean.TRUE.equals(article.getFeatured()))
        .viewCount(article.getViewCount() == null ? 0 : article.getViewCount())
        .likeCount(article.getLikeCount() == null ? 0 : article.getLikeCount())
        .favoriteCount(article.getFavoriteCount() == null ? 0 : article.getFavoriteCount())
        .shareCount(article.getShareCount() == null ? 0 : article.getShareCount())
        .commentCount(commentCount)
        .liked(currentUserId != null && hasInteraction(article.getId(), currentUserId, "like"))
        .favorited(
            currentUserId != null && hasInteraction(article.getId(), currentUserId, "favorite"))
        .rejectReason(article.getRejectReason())
        .reviewedBy(article.getReviewedBy())
        .reviewerName(displayName(reviewer))
        .reviewedAt(article.getReviewedAt())
        .publishedAt(article.getPublishedAt())
        .createdAt(article.getCreatedAt())
        .updatedAt(article.getUpdatedAt())
        .build();
  }

  private Integer currentUserIdOrNull() {
    return StpUtil.isLogin() ? StpUtil.getLoginIdAsInt() : null;
  }

  private boolean hasInteraction(Integer articleId, Integer userId, String interactionType) {
    return blogArticleInteractionMapper.selectLatestByArticleUserType(
            articleId, userId, interactionType)
        != null;
  }

  private List<ResponseBlogCommentVO> toCommentResponseList(List<BlogComment> comments) {
    if (comments == null || comments.isEmpty()) return List.of();
    List<Integer> userIds =
        comments.stream().map(BlogComment::getUserId).filter(Objects::nonNull).distinct().toList();
    Map<Integer, User> users =
        userIds.isEmpty()
            ? Map.of()
            : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    return comments.stream().map(comment -> toCommentResponse(comment, users)).toList();
  }

  private List<ResponseBlogCommentVO> toCommentTree(List<BlogComment> comments) {
    if (comments == null || comments.isEmpty()) return List.of();
    List<ResponseBlogCommentVO> flat = toCommentResponseList(comments);
    Map<Integer, ResponseBlogCommentVO> commentMap = new LinkedHashMap<>();
    for (ResponseBlogCommentVO comment : flat) {
      comment.setReplies(new ArrayList<>());
      commentMap.put(comment.getId(), comment);
    }
    List<ResponseBlogCommentVO> roots = new ArrayList<>();
    for (ResponseBlogCommentVO comment : flat) {
      Integer parentId = comment.getParentId();
      ResponseBlogCommentVO parent = parentId == null ? null : commentMap.get(parentId);
      if (parent == null) {
        roots.add(comment);
      } else {
        parent.getReplies().add(comment);
      }
    }
    for (ResponseBlogCommentVO comment : flat) {
      comment.setReplyCount(comment.getReplies() == null ? 0 : comment.getReplies().size());
    }
    return roots;
  }

  private ResponseBlogCommentVO toCommentResponse(BlogComment comment, Map<Integer, User> users) {
    User user = comment.getUserId() == null ? null : users.get(comment.getUserId());
    return ResponseBlogCommentVO.builder()
        .id(comment.getId())
        .articleId(comment.getArticleId())
        .userId(comment.getUserId())
        .parentId(comment.getParentId())
        .userName(displayName(user))
        .userAvatar(user == null ? null : user.getAvatar())
        .content(comment.getContent())
        .status(comment.getStatus())
        .replyCount(0)
        .replies(new ArrayList<>())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }

  private String displayName(User user) {
    if (user == null) return null;
    String realName = trimToNull(user.getRealName());
    return realName == null ? user.getUserName() : realName;
  }
}
