package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateClubStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateRecruitmentStatusDTO;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.service.ClubService;
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
public class ClubController {
  private final ClubService clubService;

  @GetMapping("/clubs")
  @SaCheckPermission("club:list")
  public ApiResponse<List<Club>> getClubs(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String recruitmentStatus) {
    return clubService.getClubs(keyword, category, status, recruitmentStatus);
  }

  @PostMapping("/clubs")
  @SaCheckPermission("club:create")
  public ApiResponse<String> createClub(
      @Valid @RequestBody RequestCreateClubDTO requestCreateClubDTO) {
    return clubService.createClub(requestCreateClubDTO);
  }

  @GetMapping("/clubs/{clubId}")
  @SaCheckPermission("club:detail")
  public ApiResponse<Club> getClubById(@PathVariable Integer clubId) {
    return clubService.getClubById(clubId);
  }

  @PatchMapping("/clubs/{clubId}")
  @SaCheckPermission("club:update")
  public ApiResponse<String> updateClub(
      @PathVariable Integer clubId, @Valid @RequestBody RequestUpdateClubDTO requestUpdateClubDTO) {
    return clubService.updateClub(clubId, requestUpdateClubDTO);
  }

  @PatchMapping("/clubs/{clubId}/status")
  @SaCheckPermission("club:status:update")
  public ApiResponse<String> updateStatus(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestUpdateClubStatusDTO requestUpdateClubStatusDTO) {
    return clubService.updateStatus(clubId, requestUpdateClubStatusDTO);
  }

  @PatchMapping("/clubs/{clubId}/recruitment-status")
  @SaCheckPermission("club:recruitment-status:update")
  public ApiResponse<String> updateRecruitmentStatus(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestUpdateRecruitmentStatusDTO requestUpdateRecruitmentStatusDTO) {
    return clubService.updateRecruitmentStatus(clubId, requestUpdateRecruitmentStatusDTO);
  }
}
