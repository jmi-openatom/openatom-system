package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateBlogCommentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBlogInteractionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestReviewBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateBlogArticleDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogArticleVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogCommentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseBlogInteractionVO;
import java.util.List;

/**
 * 博客服务接口
 *
 * <p>定义公开浏览、正式成员创作和管理员管控相关业务操作
 */
public interface BlogService {
  Result<PageDataVO<ResponseBlogArticleVO>> publicArticles(
      String keyword, String category, String tag, Long page, Long pageSize);

  Result<ResponseBlogArticleVO> publicDetail(Integer articleId);

  Result<List<String>> publicCategories();

  Result<List<ResponseBlogCommentVO>> publicComments(Integer articleId);

  Result<String> createComment(Integer articleId, RequestCreateBlogCommentDTO request);

  Result<ResponseBlogArticleVO> likeArticle(Integer articleId, RequestBlogInteractionDTO request);

  Result<ResponseBlogArticleVO> favoriteArticle(Integer articleId, RequestBlogInteractionDTO request);

  Result<ResponseBlogArticleVO> shareArticle(Integer articleId, RequestBlogInteractionDTO request);

  Result<PageDataVO<ResponseBlogArticleVO>> myArticles(String status, Long page, Long pageSize);

  Result<ResponseBlogArticleVO> createArticle(RequestCreateBlogArticleDTO request);

  Result<String> updateOwnArticle(Integer articleId, RequestUpdateBlogArticleDTO request);

  Result<String> publishOwnArticle(Integer articleId);

  Result<String> deleteOwnArticle(Integer articleId);

  Result<PageDataVO<ResponseBlogArticleVO>> adminArticles(
      String status, String keyword, Long page, Long pageSize);

  Result<String> adminReviewArticle(Integer articleId, RequestReviewBlogArticleDTO request);

  Result<String> adminDeleteArticle(Integer articleId);

  Result<List<ResponseBlogCommentVO>> adminComments(Integer articleId);

  Result<String> adminUpdateCommentStatus(Integer commentId, String status);

  Result<PageDataVO<ResponseBlogInteractionVO>> adminInteractions(
      String interactionType, Integer articleId, Long page, Long pageSize);
}
