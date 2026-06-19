package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSaveClubRegulationDTO;
import edu.jmi.openatom.server.openatomsystem.service.ClubRegulationService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseClubRegulationVO;
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

/** 社团规章制度控制器 */
@RestController
@RequiredArgsConstructor
public class ClubRegulationController {
  private final ClubRegulationService clubRegulationService;

  @GetMapping("/regulations")
  @SaCheckPermission("regulation:list")
  public Result<List<ResponseClubRegulationVO>> list(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    return clubRegulationService.list(clubId, status, keyword);
  }

  @GetMapping("/regulations/{regulationId}")
  @SaCheckPermission("regulation:list")
  public Result<ResponseClubRegulationVO> detail(@PathVariable Integer regulationId) {
    return clubRegulationService.detail(regulationId);
  }

  @PostMapping("/clubs/{clubId}/regulations")
  @SaCheckPermission("regulation:create")
  public Result<ResponseClubRegulationVO> create(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestSaveClubRegulationDTO request) {
    return clubRegulationService.create(clubId, request);
  }

  @PatchMapping("/regulations/{regulationId}")
  @SaCheckPermission("regulation:update")
  public Result<String> update(
      @PathVariable Integer regulationId,
      @Valid @RequestBody RequestSaveClubRegulationDTO request) {
    return clubRegulationService.update(regulationId, request);
  }

  @DeleteMapping("/regulations/{regulationId}")
  @SaCheckPermission("regulation:delete")
  public Result<String> delete(@PathVariable Integer regulationId) {
    return clubRegulationService.delete(regulationId);
  }

  @GetMapping("/site/regulations")
  public Result<List<ResponseClubRegulationVO>> publicList(
      @RequestParam(required = false) Integer clubId,
      @RequestParam(required = false) String keyword) {
    return clubRegulationService.publicList(clubId, keyword);
  }

  @GetMapping("/site/regulations/{regulationId}")
  public Result<ResponseClubRegulationVO> publicDetail(@PathVariable Integer regulationId) {
    return clubRegulationService.publicDetail(regulationId);
  }
}
