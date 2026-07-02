package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl.DirectoryStats;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl.ExportResult;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl.ImportResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件迁移控制器
 *
 * <p>提供文件资源（头像、图床图片、请假附件、文档模板、生成文档）的导出和导入能力，
 * 用于服务器迁移场景。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/file-migration")
public class FileMigrationController {
  private final FileMigrationServiceImpl fileMigrationService;

  /** 获取各存储目录的文件统计信息。 */
  @GetMapping("/stats")
  @SaCheckPermission("file:migration:stats")
  public Result<List<DirectoryStats>> stats() {
    return Result.success(fileMigrationService.stats());
  }

  /** 导出所有文件资源为 ZIP 下载。 */
  @GetMapping("/export")
  @SaCheckPermission("file:migration:export")
  public ResponseEntity<byte[]> export() {
    try {
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
      ExportResult result = fileMigrationService.exportAll(baos);
      String fileName =
          "openatom-files-"
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
              + ".zip";
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              ContentDisposition.attachment().filename(fileName, java.nio.charset.StandardCharsets.UTF_8).build().toString())
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .header("X-Export-Total-Files", String.valueOf(result.getTotalFiles()))
          .header("X-Export-Total-Size", String.valueOf(result.getTotalSize()))
          .body(baos.toByteArray());
    } catch (IOException e) {
      log.error("文件资源导出失败", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 导入 ZIP 文件恢复文件资源。 */
  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SaCheckPermission("file:migration:import")
  public Result<ImportResult> importZip(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) {
    if (file == null || file.isEmpty()) {
      return Result.error(400, "请选择 ZIP 文件");
    }
    String originalName = file.getOriginalFilename();
    if (originalName == null || !originalName.toLowerCase().endsWith(".zip")) {
      return Result.error(400, "仅支持 ZIP 文件");
    }
    Path tempZip = null;
    try {
      tempZip = Files.createTempFile("openatom-import-", ".zip");
      try (var is = file.getInputStream()) {
        Files.copy(is, tempZip, StandardCopyOption.REPLACE_EXISTING);
      }
      ImportResult result = fileMigrationService.importZip(tempZip, overwrite);
      return Result.success(result, "导入完成: " + result.getImportedFiles() + " 个文件, 跳过 " + result.getSkippedFiles() + " 个");
    } catch (IOException e) {
      log.error("文件资源导入失败", e);
      return Result.error(400, "导入失败: " + e.getMessage());
    } finally {
      if (tempZip != null) {
        try {
          Files.deleteIfExists(tempZip);
        } catch (IOException ignored) {
        }
      }
    }
  }
}
