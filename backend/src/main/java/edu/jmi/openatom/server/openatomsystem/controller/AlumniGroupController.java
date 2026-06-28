package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateAlumniGroupDTO;
import edu.jmi.openatom.server.openatomsystem.entity.ClubAlumniGroup;
import edu.jmi.openatom.server.openatomsystem.service.AlumniGroupService;
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
 * 往届管理人员分组控制器
 *
 * <p>提供分组的增删改查操作
 */
@RestController
@RequiredArgsConstructor
public class AlumniGroupController {
  private final AlumniGroupService alumniGroupService;

  /**
   * 按社团ID获取分组列表
   *
   * @param clubId 社团ID
   * @return 分组列表
   */
  @GetMapping("/clubs/{clubId}/alumni-groups")
  @SaCheckPermission("membership:list")
  public Result<List<ClubAlumniGroup>> listByClubId(@PathVariable Integer clubId) {
    return alumniGroupService.listByClubId(clubId);
  }

  /**
   * 创建分组
   *
   * @param clubId 社团ID
   * @param request 创建分组请求
   * @return 操作结果
   */
  @PostMapping("/clubs/{clubId}/alumni-groups")
  @SaCheckPermission("membership:create")
  public Result<String> create(
      @PathVariable Integer clubId,
      @Valid @RequestBody RequestCreateAlumniGroupDTO request) {
    return alumniGroupService.create(clubId, request);
  }

  /**
   * 更新分组
   *
   * @param groupId 分组ID
   * @param request 更新分组请求
   * @return 操作结果
   */
  @PatchMapping("/alumni-groups/{groupId}")
  @SaCheckPermission("membership:update")
  public Result<String> update(
      @PathVariable Integer groupId,
      @Valid @RequestBody RequestUpdateAlumniGroupDTO request) {
    return alumniGroupService.update(groupId, request);
  }

  /**
   * 删除分组
   *
   * @param groupId 分组ID
   * @return 操作结果
   */
  @DeleteMapping("/alumni-groups/{groupId}")
  @SaCheckPermission("membership:update")
  public Result<String> delete(@PathVariable Integer groupId) {
    return alumniGroupService.delete(groupId);
  }
}
