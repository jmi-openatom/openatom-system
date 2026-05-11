package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdatePositionDTO;
import edu.jmi.openatom.server.openatomsystem.dto.response.ResponsePositionDTO;
import edu.jmi.openatom.server.openatomsystem.service.PositionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 职位管理控制器
 *
 * <p>提供社团职位的查询, 创建, 详情, 更新及删除等功能
 */
@RestController
@RequiredArgsConstructor
public class PositionController {
  private final PositionService positionService;

  /**
   * 根据社团 ID 获取职位列表
   *
   * @param clubId 社团ID
   * @return 职位列表
   */
  @GetMapping("/clubs/{clubId}/positions")
  @SaCheckPermission("position:list")
  public ApiResponse<List<ResponsePositionDTO>> getPositionsByClubId(@PathVariable Integer clubId) {
    return positionService.getPositionsByClubId(clubId);
  }

  /**
   * 创建社团职位
   *
   * @param clubId 社团ID
   * @param requestCreatePositionDTO 创建职位请求参数
   * @return 创建结果
   */
  @PostMapping("/clubs/{clubId}/positions")
  @SaCheckPermission("position:create")
  public ApiResponse<String> createPosition(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestCreatePositionDTO requestCreatePositionDTO) {
    return positionService.createPosition(clubId, requestCreatePositionDTO);
  }

  /**
   * 根据职位 ID 获取职位详情
   *
   * @param positionId 职位ID
   * @return 职位详情
   */
  @GetMapping("/positions/{positionId}")
  @SaCheckPermission("position:detail")
  public ApiResponse<ResponsePositionDTO> getPositionById(@PathVariable Integer positionId) {
    return positionService.getPositionById(positionId);
  }

  /**
   * 更新职位信息
   *
   * @param positionId 职位ID
   * @param requestUpdatePositionDTO 更新职位请求参数
   * @return 更新结果
   */
  @PatchMapping("/positions/{positionId}")
  @SaCheckPermission("position:update")
  public ApiResponse<String> updatePosition(
      @PathVariable Integer positionId,
      @Valid @RequestBody RequestUpdatePositionDTO requestUpdatePositionDTO) {
    return positionService.updatePosition(positionId, requestUpdatePositionDTO);
  }

  /**
   * 删除职位
   *
   * @param positionId 职位ID
   * @return 删除结果
   */
  @DeleteMapping("/positions/{positionId}")
  @SaCheckPermission("position:delete")
  public ApiResponse<String> deletePosition(@PathVariable Integer positionId) {
    return positionService.deletePosition(positionId);
  }
}
