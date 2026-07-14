package edu.jmi.openatom.server.openatomsystem.controller;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSavePartnerClubDTO;
import edu.jmi.openatom.server.openatomsystem.entity.PartnerClub;
import edu.jmi.openatom.server.openatomsystem.service.PartnerClubService;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import edu.jmi.openatom.server.openatomsystem.vo.ResponsePartnerClubVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 官网开源伙伴公开接口。 */
@RestController
@RequiredArgsConstructor
public class PartnerClubController {
  private final PartnerClubService partnerClubService;

  @GetMapping("/site/partner-clubs")
  public Result<List<ResponsePartnerClubVO>> publicList(
      @RequestParam(required = false) Boolean featured,
      @RequestParam(required = false) Integer limit) {
    return partnerClubService.publicList(featured, limit);
  }

  @GetMapping("/partner-clubs")
  @SaCheckPermission("partner-club:list")
  public Result<PageDataVO<PartnerClub>> adminList(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean featured,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return partnerClubService.adminList(keyword, status, featured, page, pageSize);
  }

  @PostMapping("/partner-clubs")
  @SaCheckPermission("partner-club:create")
  public Result<PartnerClub> create(@Valid @RequestBody RequestSavePartnerClubDTO request) {
    return partnerClubService.create(request);
  }

  @PatchMapping("/partner-clubs/{partnerClubId}")
  @SaCheckPermission("partner-club:update")
  public Result<PartnerClub> update(
      @PathVariable Integer partnerClubId,
      @Valid @RequestBody RequestSavePartnerClubDTO request) {
    return partnerClubService.update(partnerClubId, request);
  }

  @PatchMapping("/partner-clubs/{partnerClubId}/status")
  @SaCheckPermission("partner-club:status:update")
  public Result<PartnerClub> updateStatus(
      @PathVariable Integer partnerClubId, @RequestParam String status) {
    return partnerClubService.updateStatus(partnerClubId, status);
  }

  @DeleteMapping("/partner-clubs/{partnerClubId}")
  @SaCheckPermission("partner-club:delete")
  public Result<String> delete(@PathVariable Integer partnerClubId) {
    return partnerClubService.delete(partnerClubId);
  }
}
