package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.impl.ImageHostingStorageServiceImpl;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 图床控制器
 *
 * <p>提供博客图片上传能力，图片读取由 FileController 公开提供
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
      ImageHostingStorageServiceImpl.StoredImage image = imageHostingStorageService.store(file);
      String imageBaseUrl =
          ServletUriComponentsBuilder.fromCurrentContextPath().path("/files/images/").toUriString();
      String url = imageBaseUrl + image.getFileName();
      return Result.success(
          ResponseImageUploadVO.builder()
              .fileName(image.getFileName())
              .url(url)
              .markdown("![图片](" + url + ")")
              .build(),
          "图片上传成功");
    } catch (IOException e) {
      return Result.error(400, e.getMessage());
    }
  }
}
