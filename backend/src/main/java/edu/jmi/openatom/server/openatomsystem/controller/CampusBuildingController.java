package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.impl.CampusBuildingPhotoStorage;
import edu.jmi.openatom.server.openatomsystem.service.impl.CampusBuildingService;
import edu.jmi.openatom.server.openatomsystem.service.impl.CampusBuildingService.BuildingDetail;
import edu.jmi.openatom.server.openatomsystem.service.impl.CampusBuildingService.Submission;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class CampusBuildingController {
  private final CampusBuildingService service;
  private final CampusBuildingPhotoStorage storage;

  @GetMapping("/site/campus-buildings/{buildingId}")
  public Result<BuildingDetail> detail(@PathVariable String buildingId) {
    return Result.success(service.publicDetail(buildingId, baseUrl()));
  }

  @PostMapping(value = "/campus-buildings/{buildingId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SaCheckLogin
  public Result<Long> submit(@PathVariable String buildingId,
      @RequestParam(required = false) String description,
      @RequestParam(defaultValue = "false") boolean replaceDescription,
      @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
      @RequestParam(value = "removedPhotoIds", required = false) List<Long> removedPhotoIds) {
    try {
      return Result.success(service.submit(buildingId, description, replaceDescription, photos, removedPhotoIds,
          StpUtil.getLoginIdAsInt()), "已提交，审核通过后展示");
    } catch (IOException e) {
      return Result.error(400, "图片保存失败，请重新提交");
    }
  }

  @GetMapping("/campus-buildings/{buildingId}/submissions/my")
  @SaCheckLogin
  public Result<PageDataVO<Submission>> mySubmissions(@PathVariable String buildingId,
      @RequestParam(required = false) Long page, @RequestParam(required = false) Long pageSize) {
    return Result.success(service.submissions(buildingId, StpUtil.getLoginIdAsInt(), null, page, pageSize, baseUrl()));
  }

  @GetMapping("/campus-buildings/admin/submissions")
  @SaCheckPermission("campus-building:list")
  public Result<PageDataVO<Submission>> adminSubmissions(@RequestParam(required = false) String buildingId,
      @RequestParam(required = false) String status, @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return Result.success(service.submissions(buildingId, null, status, page, pageSize, baseUrl()));
  }

  @PostMapping("/campus-buildings/admin/submissions/{id}/review")
  @SaCheckPermission("campus-building:review")
  public Result<Void> review(@PathVariable long id, @RequestBody ReviewRequest request) {
    service.review(id, request.action(), request.reason(), StpUtil.getLoginIdAsInt());
    return Result.success();
  }

  @GetMapping("/site/campus-building-photos/{id}")
  public ResponseEntity<Resource> publicPhoto(@PathVariable long id) {
    return photo(id, null, false, true);
  }

  @GetMapping("/campus-buildings/photos/{id}")
  @SaCheckLogin
  public ResponseEntity<Resource> privatePhoto(@PathVariable long id) {
    boolean reviewer = StpUtil.hasPermission("campus-building:list") || StpUtil.hasPermission("campus-building:review");
    return photo(id, StpUtil.getLoginIdAsInt(), reviewer, false);
  }

  private ResponseEntity<Resource> photo(long id, Integer userId, boolean reviewer, boolean publicOnly) {
    var photo = service.photo(id, userId, reviewer, publicOnly);
    if (photo.isEmpty()) return ResponseEntity.notFound().build();
    try {
      return ResponseEntity.ok().cacheControl(CacheControl.noStore())
          .header("X-Content-Type-Options", "nosniff")
          .contentType(MediaType.parseMediaType(photo.get().mimeType()))
          .body(storage.load(photo.get().storageName()));
    } catch (IOException e) {
      return ResponseEntity.notFound().build();
    }
  }

  private String baseUrl() {
    return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
  }

  public record ReviewRequest(String action, String reason) {}
}
