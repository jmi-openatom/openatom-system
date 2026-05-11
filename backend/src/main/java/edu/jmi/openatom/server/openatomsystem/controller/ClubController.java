package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateClubStatusDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateRecruitmentStatusDTO;
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

/**
 * 社团控制器
 *
 * <p>提供社团的增删改查, 状态更新及招新状态更新等操作
 */
@RestController
@RequiredArgsConstructor
public class ClubController {
  private final ClubService clubService;

  /**
   * 获取社团列表
   *
   * @param keyword 关键词
   * @param category 社团分类
   * @param status 社团状态
   * @param recruitmentStatus 招新状态
   * @return 社团列表
   */
  @GetMapping("/clubs")
  @SaCheckPermission("club:list")
  public Result<List<Club>> getClubs(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String recruitmentStatus) {
    return clubService.getClubs(keyword, category, status, recruitmentStatus);
  }

  /**
   * 创建社团
   *
   * @param requestCreateClubDTO 创建社团请求
   * @return 操作结果
   */
  @PostMapping("/clubs")
  @SaCheckPermission("club:create")
  public Result<String> createClub(
      @Valid @RequestBody RequestCreateClubDTO requestCreateClubDTO) {
    return clubService.createClub(requestCreateClubDTO);
  }

  /**
   * 根据ID获取社团详情
   *
   * @param clubId 社团ID
   * @return 社团详情
   */
  @GetMapping("/clubs/{clubId}")
  @SaCheckPermission("club:detail")
  public Result<Club> getClubById(@PathVariable Integer clubId) {
    return clubService.getClubById(clubId);
  }

  /**
   * 更新社团信息
   *
   * @param clubId 社团ID
   * @param requestUpdateClubDTO 更新社团请求
   * @return 操作结果
   */
  @PatchMapping("/clubs/{clubId}")
  @SaCheckPermission("club:update")
  public Result<String> updateClub(
      @PathVariable Integer clubId, @Valid @RequestBody RequestUpdateClubDTO requestUpdateClubDTO) {
    return clubService.updateClub(clubId, requestUpdateClubDTO);
  }

  /**
   * 更新社团状态
   *
   * @param clubId 社团ID
   * @param requestUpdateClubStatusDTO 更新状态请求
   * @return 操作结果
   */
  @PatchMapping("/clubs/{clubId}/status")
  @SaCheckPermission("club:status:update")
  public Result<String> updateStatus(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestUpdateClubStatusDTO requestUpdateClubStatusDTO) {
    return clubService.updateStatus(clubId, requestUpdateClubStatusDTO);
  }

  /**
   * 更新社团招新状态
   *
   * @param clubId 社团ID
   * @param requestUpdateRecruitmentStatusDTO 更新招新状态请求
   * @return 操作结果
   */
  @PatchMapping("/clubs/{clubId}/recruitment-status")
  @SaCheckPermission("club:recruitment-status:update")
  public Result<String> updateRecruitmentStatus(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestUpdateRecruitmentStatusDTO requestUpdateRecruitmentStatusDTO) {
    return clubService.updateRecruitmentStatus(clubId, requestUpdateRecruitmentStatusDTO);
  }
}
