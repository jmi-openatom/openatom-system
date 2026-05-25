package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateBlogCommentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateBlogCommentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.service.BlogService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogArticleVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogCommentVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客控制器
 *
 * <p>提供公开博客浏览、正式成员创作和管理员管控接口
 */
@RestController
@RequiredArgsConstructor
public class BlogController {
  private final BlogService blogService;

  @GetMapping("/site/blog/articles")
  public Result<PageDataVO<ResponseBlogArticleVO>> publicArticles(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String tag,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return blogService.publicArticles(keyword, category, tag, page, pageSize);
  }

  @GetMapping("/site/blog/articles/{articleId}")
  public Result<ResponseBlogArticleVO> publicDetail(@PathVariable Integer articleId) {
    return blogService.publicDetail(articleId);
  }

  @GetMapping("/site/blog/categories")
  public Result<List<String>> publicCategories() {
    return blogService.publicCategories();
  }

  @GetMapping("/site/blog/articles/{articleId}/comments")
  public Result<List<ResponseBlogCommentVO>> publicComments(@PathVariable Integer articleId) {
    return blogService.publicComments(articleId);
  }

  @PostMapping("/site/blog/articles/{articleId}/comments")
  public Result<String> createComment(
      @PathVariable Integer articleId, @Valid @RequestBody RequestCreateBlogCommentDTO request) {
    return blogService.createComment(articleId, request);
  }

  @GetMapping("/blog/my/articles")
  public Result<PageDataVO<ResponseBlogArticleVO>> myArticles(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return blogService.myArticles(status, page, pageSize);
  }

  @PostMapping("/blog/articles")
  public Result<ResponseBlogArticleVO> createArticle(
      @Valid @RequestBody RequestCreateBlogArticleDTO request) {
    return blogService.createArticle(request);
  }

  @PatchMapping("/blog/articles/{articleId}")
  public Result<String> updateOwnArticle(
      @PathVariable Integer articleId, @RequestBody RequestUpdateBlogArticleDTO request) {
    return blogService.updateOwnArticle(articleId, request);
  }

  @PostMapping("/blog/articles/{articleId}/publish")
  public Result<String> publishOwnArticle(@PathVariable Integer articleId) {
    return blogService.publishOwnArticle(articleId);
  }

  @DeleteMapping("/blog/articles/{articleId}")
  public Result<String> deleteOwnArticle(@PathVariable Integer articleId) {
    return blogService.deleteOwnArticle(articleId);
  }

  @GetMapping("/blog/admin/articles")
  @SaCheckPermission("blog:list")
  public Result<PageDataVO<ResponseBlogArticleVO>> adminArticles(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return blogService.adminArticles(status, keyword, page, pageSize);
  }

  @PostMapping("/blog/admin/articles/{articleId}/review")
  @SaCheckPermission("blog:review")
  public Result<String> adminReviewArticle(
      @PathVariable Integer articleId, @RequestBody RequestReviewBlogArticleDTO request) {
    return blogService.adminReviewArticle(articleId, request);
  }

  @DeleteMapping("/blog/admin/articles/{articleId}")
  @SaCheckPermission("blog:delete")
  public Result<String> adminDeleteArticle(@PathVariable Integer articleId) {
    return blogService.adminDeleteArticle(articleId);
  }

  @GetMapping("/blog/admin/articles/{articleId}/comments")
  @SaCheckPermission("blog-comment:list")
  public Result<List<ResponseBlogCommentVO>> adminComments(@PathVariable Integer articleId) {
    return blogService.adminComments(articleId);
  }

  @PatchMapping("/blog/admin/comments/{commentId}/status")
  @SaCheckPermission("blog-comment:manage")
  public Result<String> adminUpdateCommentStatus(
      @PathVariable Integer commentId,
      @Valid @RequestBody RequestUpdateBlogCommentStatusDTO request) {
    return blogService.adminUpdateCommentStatus(commentId, request.getStatus());
  }
}
