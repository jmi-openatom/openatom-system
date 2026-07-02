package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl.DirectoryStats;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl.ExportResult;
import edu.jmi.openatom.server.openatomsystem.service.impl.FileMigrationServiceImpl.ImportResult;
import edu.jmi.openatom.server.openatomsystem.service.impl.ExportTaskService;
import edu.jmi.openatom.server.openatomsystem.service.impl.ExportTaskService.ExportTask;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final ExportTaskService exportTaskService;

  /** 获取各存储目录的文件统计信息。 */
  @GetMapping("/stats")
  @SaCheckPermission("file:migration:stats")
  public Result<List<DirectoryStats>> stats() {
    return Result.success(fileMigrationService.stats());
  }

  /** 导出所有文件资源为 ZIP 下载（同步方式，保留向后兼容）。 */
  @GetMapping("/export")
  @SaCheckPermission("file:migration:export")
  public ResponseEntity<byte[]> export() {
    try {
      long startTime = System.currentTimeMillis();
      log.info("开始导出文件资源...");
      java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(1024 * 1024); // 初始容量 1MB
      ExportResult result = fileMigrationService.exportAll(baos);
      byte[] zipData = baos.toByteArray();
      long elapsed = System.currentTimeMillis() - startTime;
      log.info("文件资源导出完成: {} 个文件, 总大小 {} MB, 耗时 {} ms",
          result.getTotalFiles(), zipData.length / (1024 * 1024), elapsed);
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
          .header("X-Export-Elapsed-Ms", String.valueOf(elapsed))
          .body(zipData);
    } catch (IOException e) {
      log.error("文件资源导出失败", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 启动异步导出任务。 */
  @PostMapping("/export/start")
  @SaCheckPermission("file:migration:export")
  public Result<ExportTask> startExport() {
    try {
      ExportTask task = exportTaskService.startExport();
      return Result.success(task, "导出任务已启动");
    } catch (Exception e) {
      log.error("启动导出任务失败", e);
      String reason = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
      return Result.error(500, "启动导出任务失败: " + reason);
    }
  }

  /** 查询导出任务状态。 */
  @GetMapping("/export/status/{taskId}")
  @SaCheckPermission("file:migration:export")
  public Result<ExportTask> getExportStatus(@PathVariable String taskId) {
    try {
      ExportTask task = exportTaskService.getTaskStatus(taskId);
      return Result.success(task);
    } catch (IllegalArgumentException e) {
      return Result.error(404, e.getMessage());
    }
  }

  /** 下载已完成的导出文件。 */
  @GetMapping("/export/download/{taskId}")
  @SaCheckPermission("file:migration:export")
  public ResponseEntity<byte[]> downloadExport(@PathVariable String taskId) {
    try {
      Path filePath = exportTaskService.getExportFilePath(taskId);
      byte[] data = Files.readAllBytes(filePath);
      ExportTask task = exportTaskService.getTaskStatus(taskId);
      
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              ContentDisposition.attachment().filename(task.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .header("X-File-Size", String.valueOf(task.getFileSize()))
          .body(data);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    } catch (IOException e) {
      log.error("下载导出文件失败", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** 手动触发自动化备份。 */
  @PostMapping("/backup/run")
  @SaCheckPermission("file:migration:export")
  public Result<String> runBackup() {
    try {
      exportTaskService.autoBackupCompletedTasks();
      return Result.success(null, "备份任务已启动，请查看日志获取详细信息");
    } catch (Exception e) {
      log.error("手动触发备份失败", e);
      return Result.error(500, "备份失败: " + e.getMessage());
    }
  }

  /** 获取备份文件列表。 */
  @GetMapping("/backup/list")
  @SaCheckPermission("file:migration:export")
  public Result<java.util.List<BackupFileInfo>> listBackups() {
    try {
      java.util.List<BackupFileInfo> backups = new java.util.ArrayList<>();
      Path backupDir = Path.of("./uploads/export-backups");
      
      if (!Files.exists(backupDir)) {
        return Result.success(backups);
      }
      
      try (var stream = Files.list(backupDir)) {
        stream.filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".zip"))
            .sorted((a, b) -> {
              try {
                return Long.compare(
                    Files.getLastModifiedTime(b).toMillis(),
                    Files.getLastModifiedTime(a).toMillis()
                );
              } catch (IOException e) {
                return 0;
              }
            })
            .forEach(path -> {
              try {
                BackupFileInfo info = new BackupFileInfo();
                info.setFileName(path.getFileName().toString());
                info.setFileSize(Files.size(path));
                info.setCreateTime(LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    java.time.ZoneId.systemDefault()
                ));
                backups.add(info);
              } catch (IOException e) {
                log.warn("读取备份文件信息失败: {}", path, e);
              }
            });
      }
      
      return Result.success(backups);
    } catch (IOException e) {
      log.error("获取备份列表失败", e);
      return Result.error(500, "获取备份列表失败: " + e.getMessage());
    }
  }

  /** 删除指定备份文件。 */
  @DeleteMapping("/backup/{fileName}")
  @SaCheckPermission("file:migration:export")
  public Result<Void> deleteBackup(@PathVariable String fileName) {
    try {
      // 安全检查：只允许删除 .zip 文件，且文件名不能包含路径
      if (!fileName.endsWith(".zip") || fileName.contains("/") || fileName.contains("\\")) {
        return Result.error(400, "非法的文件名");
      }
      
      Path backupDir = Path.of("./uploads/export-backups");
      Path file = backupDir.resolve(fileName).normalize();
      
      // 确保文件在备份目录内
      if (!file.startsWith(backupDir)) {
        return Result.error(400, "非法的文件路径");
      }
      
      if (!Files.exists(file)) {
        return Result.error(404, "备份文件不存在");
      }
      
      Files.delete(file);
      log.info("删除备份文件: {}", fileName);
      return Result.success(null, "删除成功");
    } catch (IOException e) {
      log.error("删除备份文件失败: {}", fileName, e);
      return Result.error(500, "删除失败: " + e.getMessage());
    }
  }

  /** 备份文件信息。 */
  @lombok.Data
  public static class BackupFileInfo {
    private String fileName;
    private Long fileSize;
    private LocalDateTime createTime;
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
