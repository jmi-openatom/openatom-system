package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.dto.ApiResponse;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestCreateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.dto.request.RequestUpdateAwardDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAward;
import edu.jmi.openatom.server.openatomsystem.service.AwardService;
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
 * 社团奖项控制器
 *
 * <p>提供社团奖项的增删改查操作
 */
@RestController
@RequiredArgsConstructor
public class AwardController {
  private final AwardService awardService;

  /**
   * 获取奖项列表
   *
   * @return 奖项列表
   */
  @GetMapping("/awards")
  @SaCheckPermission("award:list")
  public ApiResponse<List<ClubAward>> list() {
    return awardService.list();
  }

  /**
   * 创建奖项
   *
   * @param request 创建奖项请求
   * @return 操作结果
   */
  @PostMapping("/awards")
  @SaCheckPermission("award:create")
  public ApiResponse<String> create(@Valid @RequestBody RequestCreateAwardDTO request) {
    return awardService.create(request);
  }

  /**
   * 更新奖项
   *
   * @param awardId 奖项ID
   * @param request 更新奖项请求
   * @return 操作结果
   */
  @PatchMapping("/awards/{awardId}")
  @SaCheckPermission("award:update")
  public ApiResponse<String> update(
      @PathVariable Integer awardId, @Valid @RequestBody RequestUpdateAwardDTO request) {
    return awardService.update(awardId, request);
  }

  /**
   * 删除奖项
   *
   * @param awardId 奖项ID
   * @return 操作结果
   */
  @DeleteMapping("/awards/{awardId}")
  @SaCheckPermission("award:delete")
  public ApiResponse<String> delete(@PathVariable Integer awardId) {
    return awardService.delete(awardId);
  }
}
