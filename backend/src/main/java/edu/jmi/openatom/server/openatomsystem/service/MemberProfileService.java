package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateMemberProfileCommentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveMemberProfileDTO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseAdminMemberProfileCommentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberCardVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberFilterVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileCommentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileLikeVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface MemberProfileService {
  Result<PageDataVO<ResponseMemberCardVO>> members(
      String keyword,
      Integer departmentId,
      String skill,
      String sort,
      Long page,
      Long pageSize);

  Result<ResponseMemberFilterVO> filters();

  Result<ResponseMemberProfileVO> detail(String slug);

  Result<ResponseMemberProfileLikeVO> toggleLike(String slug);

  Result<PageDataVO<ResponseMemberProfileCommentVO>> comments(
      String slug, String sort, Long page, Long pageSize);

  Result<PageDataVO<ResponseMemberProfileCommentVO>> commentReplies(
      String slug, Long rootId, Long page, Long pageSize);

  Result<ResponseMemberProfileCommentVO> createComment(
      String slug, RequestCreateMemberProfileCommentDTO request);

  Result<ResponseMemberProfileCommentVO> toggleCommentLike(String slug, Long commentId);

  Result<String> deleteOwnComment(String slug, Long commentId);

  Result<String> reportComment(String slug, Long commentId, String reason);

  Result<PageDataVO<ResponseAdminMemberProfileCommentVO>> adminComments(
      String keyword, String status, Long page, Long pageSize);

  Result<String> adminUpdateCommentStatus(Long commentId, String status);

  Result<String> adminBatchUpdateCommentStatus(List<Long> commentIds, String status);

  Result<ResponseMemberProfileVO> mine();

  Result<ResponseMemberProfileVO> save(RequestSaveMemberProfileDTO request);

  Result<ResponseMemberProfileVO> publish();

  Result<ResponseMemberProfileVO> unpublish();

  Result<ResponseImageUploadVO> upload(String kind, MultipartFile file, String baseUrl);
}
