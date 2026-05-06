package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestAssignPositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestChangeMembershipStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestFinalDecisionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateMembershipDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponseMembershipDTO;
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

@RestController
@RequiredArgsConstructor
public class MembershipController {
  private final MembershipService membershipService;

  @PostMapping("/applications/{applicationId}/final-decisions")
  @SaCheckPermission("application:final-decision")
  public ApiResponse<String> finalDecision(
      @PathVariable Integer applicationId, @Valid @RequestBody RequestFinalDecisionDTO request) {
    return membershipService.finalDecision(applicationId, request);
  }

  @GetMapping("/memberships")
  @SaCheckPermission("membership:list")
  public ApiResponse<List<ResponseMembershipDTO>> list(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(required = false) Integer positionId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    return membershipService.list(clubId, departmentId, positionId, status, keyword);
  }

  @PostMapping("/memberships")
  @SaCheckPermission("membership:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateMembershipDTO request) {
    return membershipService.create(request);
  }

  @GetMapping("/memberships/{membershipId}")
  @SaCheckPermission("membership:detail")
  public ApiResponse<ClubMembership> detail(@PathVariable Integer membershipId) {
    return membershipService.detail(membershipId);
  }

  @PatchMapping("/memberships/{membershipId}")
  @SaCheckPermission("membership:update")
  public ApiResponse<String> update(
      @PathVariable Integer membershipId, @Valid @RequestBody RequestUpdateMembershipDTO request) {
    return membershipService.update(membershipId, request);
  }

  @PostMapping("/memberships/{membershipId}/assign-position")
  @SaCheckPermission("membership:position:assign")
  public ApiResponse<String> assignPosition(
      @PathVariable Integer membershipId, @Valid @RequestBody RequestAssignPositionDTO request) {
    return membershipService.assignPosition(membershipId, request);
  }

  @PostMapping("/memberships/{membershipId}/change-status")
  @SaCheckPermission("membership:status:change")
  public ApiResponse<String> changeStatus(
      @PathVariable Integer membershipId,
      @Valid @RequestBody RequestChangeMembershipStatusDTO request) {
    return membershipService.changeStatus(membershipId, request);
  }
}
