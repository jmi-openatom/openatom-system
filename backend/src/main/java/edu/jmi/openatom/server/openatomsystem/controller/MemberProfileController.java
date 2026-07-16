package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateMemberProfileCommentDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveMemberProfileDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateBlogCommentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.service.MemberProfileService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseAdminMemberProfileCommentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberCardVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberFilterVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileCommentVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileLikeVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMemberProfileVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** 登录后的社团成员目录与个人主页接口。 */
@RestController
@RequiredArgsConstructor
public class MemberProfileController {
  private final MemberProfileService memberProfileService;

  @GetMapping("/members")
  public Result<PageDataVO<ResponseMemberCardVO>> members(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(required = false) String skill,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return memberProfileService.members(keyword, departmentId, skill, sort, page, pageSize);
  }

  @GetMapping("/members/meta/filters")
  public Result<ResponseMemberFilterVO> filters() {
    return memberProfileService.filters();
  }

  @GetMapping("/members/{slug}")
  public Result<ResponseMemberProfileVO> detail(@PathVariable String slug) {
    return memberProfileService.detail(slug);
  }

  @PostMapping("/members/{slug}/like")
  public Result<ResponseMemberProfileLikeVO> toggleLike(@PathVariable String slug) {
    return memberProfileService.toggleLike(slug);
  }

  @GetMapping("/members/{slug}/comments")
  public Result<List<ResponseMemberProfileCommentVO>> comments(@PathVariable String slug) {
    return memberProfileService.comments(slug);
  }

  @PostMapping("/members/{slug}/comments")
  public Result<String> createComment(
      @PathVariable String slug,
      @Valid @RequestBody RequestCreateMemberProfileCommentDTO request) {
    return memberProfileService.createComment(slug, request);
  }

  @GetMapping("/member-profile-comments")
  @SaCheckPermission("member-profile-comment:list")
  public Result<PageDataVO<ResponseAdminMemberProfileCommentVO>> adminComments(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return memberProfileService.adminComments(keyword, status, page, pageSize);
  }

  @PatchMapping("/member-profile-comments/{commentId}/status")
  @SaCheckPermission("member-profile-comment:manage")
  public Result<String> adminUpdateCommentStatus(
      @PathVariable Long commentId,
      @Valid @RequestBody RequestUpdateBlogCommentStatusDTO request) {
    return memberProfileService.adminUpdateCommentStatus(commentId, request.getStatus());
  }

  @GetMapping("/me/profile")
  public Result<ResponseMemberProfileVO> mine() {
    return memberProfileService.mine();
  }

  @PutMapping("/me/profile")
  public Result<ResponseMemberProfileVO> save(
      @RequestBody RequestSaveMemberProfileDTO request) {
    return memberProfileService.save(request);
  }

  @PostMapping("/me/profile/publish")
  public Result<ResponseMemberProfileVO> publish() {
    return memberProfileService.publish();
  }

  @PostMapping("/me/profile/unpublish")
  public Result<ResponseMemberProfileVO> unpublish() {
    return memberProfileService.unpublish();
  }

  @PostMapping(
      value = "/me/profile/images/{kind}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Result<ResponseImageUploadVO> upload(
      @PathVariable String kind, @RequestParam("file") MultipartFile file) {
    String imageBaseUrl =
        ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/images/").toUriString();
    return memberProfileService.upload(kind, file, imageBaseUrl);
  }
}
