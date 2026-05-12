package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestAssignPositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestBatchCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestFinalDecisionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseMembershipVO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubMembership;
import edu.jmi.openatom.server.openatomsystem.service.MembershipService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 社团成员管理控制器
 *
 * <p>提供社团成员申请的终审, 成员列表查询, 成员信息增删改查, 职位分配及状态变更等功能
 */
@RestController
@RequiredArgsConstructor
public class MembershipController {
  private final MembershipService membershipService;

  /**
   * 对社团申请作出终审决定
   *
   * @param applicationId 申请ID
   * @param request 终审决定请求参数
   * @return 终审结果
   */
  @PostMapping("/applications/{applicationId}/final-decisions")
  @SaCheckPermission("application:final-decision")
  public Result<String> finalDecision(
      @PathVariable Integer applicationId, @Valid @RequestBody RequestFinalDecisionDTO request) {
    return membershipService.finalDecision(applicationId, request);
  }

  /**
   * 分页查询成员列表
   *
   * @param clubId 社团ID（可选）
   * @param departmentId 部门ID（可选）
   * @param positionId 职位ID（可选）
   * @param status 成员状态（可选）
   * @param keyword 搜索关键词（可选）
   * @return 成员列表
   */
  @GetMapping("/memberships")
  @SaCheckPermission("membership:list")
  public Result<List<ResponseMembershipVO>> list(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(required = false) Integer positionId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    return membershipService.list(clubId, departmentId, positionId, status, keyword);
  }

  /**
   * 创建新的社团成员
   *
   * @param request 创建成员请求参数
   * @return 创建结果
   */
  @PostMapping("/memberships")
  @SaCheckPermission("membership:create")
  public Result<String> create(@Valid @RequestBody RequestCreateMembershipDTO request) {
    return membershipService.create(request);
  }

  /**
   * 获取成员详情
   *
   * @param membershipId 成员ID
   * @return 成员详情
   */
  @GetMapping("/memberships/{membershipId}")
  @SaCheckPermission("membership:detail")
  public Result<ClubMembership> detail(@PathVariable Integer membershipId) {
    return membershipService.detail(membershipId);
  }

  /**
   * 更新成员信息
   *
   * @param membershipId 成员ID
   * @param request 更新成员请求参数
   * @return 更新结果
   */
  @PatchMapping("/memberships/{membershipId}")
  @SaCheckPermission("membership:update")
  public Result<String> update(
      @PathVariable Integer membershipId, @Valid @RequestBody RequestUpdateMembershipDTO request) {
    return membershipService.update(membershipId, request);
  }

  /**
   * 批量变更成员状态
   *
   * @param request 批量变更状态请求参数
   * @return 变更结果
   */
  @PostMapping("/memberships/batch-change-status")
  @SaCheckPermission("membership:batch-change-status")
  public Result<String> batchChangeStatus(
      @Valid @RequestBody RequestBatchChangeMembershipStatusDTO request) {
    return membershipService.batchChangeStatus(request);
  }

  /**
   * 批量创建成员关系
   *
   * @param request 批量创建成员请求参数
   * @return 创建结果
   */
  @PostMapping("/memberships/batch-create")
  @SaCheckPermission("membership:batch-create")
  public Result<String> batchCreate(
      @Valid @RequestBody RequestBatchCreateMembershipDTO request) {
    return membershipService.batchCreate(request);
  }

  /**
   * 为成员分配职位
   *
   * @param membershipId 成员ID
   * @param request 分配职位请求参数
   * @return 分配结果
   */
  @PostMapping("/memberships/{membershipId}/assign-position")
  @SaCheckPermission("membership:position:assign")
  public Result<String> assignPosition(
      @PathVariable Integer membershipId, @Valid @RequestBody RequestAssignPositionDTO request) {
    return membershipService.assignPosition(membershipId, request);
  }

  /**
   * 变更成员状态
   *
   * @param membershipId 成员ID
   * @param request 变更状态请求参数
   * @return 变更结果
   */
  @PostMapping("/memberships/{membershipId}/change-status")
  @SaCheckPermission("membership:status:change")
  public Result<String> changeStatus(
      @PathVariable Integer membershipId,
      @Valid @RequestBody RequestChangeMembershipStatusDTO request) {
    return membershipService.changeStatus(membershipId, request);
  }
}
