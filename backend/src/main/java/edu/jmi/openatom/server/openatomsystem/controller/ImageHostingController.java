package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.impl.ImageHostingStorageServiceImpl;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageHostingAssetVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 图床控制器
 *
 * <p>提供独立图床上传、个人列表和后台管理能力，图片读取由 FileController 公开提供
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/image-hosting")
public class ImageHostingController {
  private final ImageHostingStorageServiceImpl imageHostingStorageService;

  @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SaCheckPermission("image:upload")
  public Result<ResponseImageUploadVO> upload(@RequestParam("file") MultipartFile file) {
    try {
      String imageBaseUrl =
          ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/images/").toUriString();
      ResponseImageUploadVO image =
          imageHostingStorageService.upload(file, imageBaseUrl, StpUtil.getLoginIdAsInt());
      return Result.success(image, "图片上传成功");
    } catch (IOException e) {
      return Result.error(400, e.getMessage());
    }
  }

  @GetMapping("/images/my")
  @SaCheckPermission("image:upload")
  public Result<PageDataVO<ResponseImageHostingAssetVO>> myImages(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return Result.success(
        imageHostingStorageService.myImages(
            StpUtil.getLoginIdAsInt(), keyword, page, pageSize));
  }

  @DeleteMapping("/images/{imageId}")
  @SaCheckPermission("image:upload")
  public Result<String> deleteOwnImage(@PathVariable Integer imageId) {
    try {
      boolean deleted =
          imageHostingStorageService.deleteOwnImage(imageId, StpUtil.getLoginIdAsInt());
      return deleted ? Result.success("图片已删除") : Result.error(404, "图片不存在或无权删除");
    } catch (IOException e) {
      return Result.error(400, e.getMessage());
    }
  }

  @GetMapping("/admin/images")
  @SaCheckPermission("image:list")
  public Result<PageDataVO<ResponseImageHostingAssetVO>> adminImages(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long page,
      @RequestParam(required = false) Long pageSize) {
    return Result.success(imageHostingStorageService.adminImages(keyword, status, page, pageSize));
  }

  @DeleteMapping("/admin/images/{imageId}")
  @SaCheckPermission("image:delete")
  public Result<String> adminDeleteImage(@PathVariable Integer imageId) {
    try {
      boolean deleted =
          imageHostingStorageService.adminDeleteImage(imageId, StpUtil.getLoginIdAsInt());
      return deleted ? Result.success("图片已删除") : Result.error(404, "图片不存在");
    } catch (IOException e) {
      return Result.error(400, e.getMessage());
    }
  }
}
