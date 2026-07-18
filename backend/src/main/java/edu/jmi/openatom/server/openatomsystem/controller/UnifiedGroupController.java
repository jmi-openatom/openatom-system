package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestCreateUnifiedGroupDTO;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateUnifiedGroupDTO;
import edu.jmi.openatom.server.openatomsystem.service.UnifiedGroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/groups")
@RequiredArgsConstructor
public class UnifiedGroupController {
  private final UnifiedGroupService unifiedGroupService;

  @GetMapping
  @SaCheckPermission("group:list")
  public Result<List<Map<String, Object>>> list(
      @RequestParam Integer clubId,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String keyword) {
    return unifiedGroupService.list(clubId, type, keyword);
  }

  @PostMapping
  @SaCheckPermission("group:create")
  public Result<String> create(@Valid @RequestBody RequestCreateUnifiedGroupDTO request) {
    return unifiedGroupService.create(request);
  }

  @PutMapping("/{groupId}")
  @SaCheckPermission("group:update")
  public Result<String> update(
      @PathVariable Long groupId, @Valid @RequestBody RequestUpdateUnifiedGroupDTO request) {
    return unifiedGroupService.update(groupId, request);
  }

  @GetMapping("/user-options")
  @SaCheckPermission("group:create")
  public Result<Map<String, Object>> userOptions(
      @RequestParam Integer clubId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer departmentId,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "20") Integer pageSize) {
    return unifiedGroupService.userOptions(clubId, keyword, status, departmentId, page, pageSize);
  }

  @GetMapping("/user-options/ids")
  @SaCheckPermission("group:create")
  public Result<List<Integer>> userOptionIds(
      @RequestParam Integer clubId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer departmentId) {
    return unifiedGroupService.userOptionIds(clubId, keyword, status, departmentId);
  }

  @GetMapping("/{groupId}")
  @SaCheckPermission("group:detail")
  public Result<Map<String, Object>> detail(@PathVariable Long groupId) {
    return unifiedGroupService.detail(groupId);
  }

  @GetMapping("/{groupId}/members")
  @SaCheckPermission("group:detail")
  public Result<List<Map<String, Object>>> members(@PathVariable Long groupId) {
    return unifiedGroupService.members(groupId);
  }

  @GetMapping("/{groupId}/dependencies")
  @SaCheckPermission("group:detail")
  public Result<Map<String, Object>> dependencies(@PathVariable Long groupId) {
    return unifiedGroupService.dependencies(groupId);
  }

  @PostMapping("/sync")
  @SaCheckPermission("group:sync")
  public Result<String> synchronize() {
    return unifiedGroupService.synchronize();
  }
}
