package edu.jmi.openatom.server.openatomsystem.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 文件迁移服务
 *
 * <p>将系统中所有上传文件资源（头像、图床图片、请假附件、文档模板、生成文档）打包为 ZIP
 * 用于服务器迁移，也支持从 ZIP 导入恢复文件。
 */
@Slf4j
@Service
public class FileMigrationServiceImpl {

  @Value("${app.avatar.storage-dir:./uploads/avatars}")
  private String avatarStorageDir;

  @Value("${app.image-hosting.storage-dir:./uploads/images}")
  private String imageStorageDir;

  @Value("${app.leave-attachment.storage-dir:./uploads/leave-attachments}")
  private String leaveAttachmentStorageDir;

  @Value("${app.document-template.storage-dir:./uploads/document-templates}")
  private String documentTemplateStorageDir;

  @Value("${app.generated-document.storage-dir:./uploads/generated-documents}")
  private String generatedDocumentStorageDir;

  /** 导出所有文件资源为 ZIP，写入指定输出流。 */
  public ExportResult exportAll(OutputStream out) throws IOException {
    Map<String, Path> directories = collectDirectories();
    long totalFiles = 0;
    long totalSize = 0;
    // 使用 BufferedOutputStream 提高写入效率
    try (java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(out, 8192);
         ZipOutputStream zos = new ZipOutputStream(bos)) {
      // 设置压缩级别（0-9），6 是平衡速度和压缩率的选择
      zos.setLevel(6);
      for (Map.Entry<String, Path> entry : directories.entrySet()) {
        String prefix = entry.getKey();
        Path dir = entry.getValue();
        if (!Files.exists(dir)) {
          continue;
        }
        FileCounter counter = new FileCounter();
        addDirectoryToZip(zos, dir, prefix, counter);
        totalFiles += counter.count;
        totalSize += counter.size;
      }
      // 写入清单文件
      writeManifest(zos, directories, totalFiles, totalSize);
      zos.finish(); // 确保 ZIP 结构完整
    }
    log.info("文件资源导出完成: {} 个文件, 总大小 {} 字节", totalFiles, totalSize);
    return new ExportResult(totalFiles, totalSize);
  }

  /** 获取各存储目录的文件统计信息。 */
  public List<DirectoryStats> stats() {
    Map<String, Path> directories = collectDirectories();
    List<DirectoryStats> result = new ArrayList<>();
    for (Map.Entry<String, Path> entry : directories.entrySet()) {
      String name = entry.getKey();
      Path dir = entry.getValue();
      if (!Files.exists(dir)) {
        result.add(new DirectoryStats(name, resolveDisplayPath(dir), 0, 0, false));
        continue;
      }
      FileCounter counter = new FileCounter();
      try {
        Files.walkFileTree(
            dir,
            new SimpleFileVisitor<>() {
              @Override
              public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                counter.count++;
                counter.size += attrs.size();
                return FileVisitResult.CONTINUE;
              }
            });
      } catch (IOException e) {
        log.warn("统计目录 {} 失败: {}", dir, e.getMessage());
      }
      result.add(
          new DirectoryStats(
              name, resolveDisplayPath(dir), counter.count, counter.size, true));
    }
    return result;
  }

  /** 从 ZIP 导入文件资源，恢复到对应存储目录。 */
  public ImportResult importZip(Path zipPath, boolean overwrite) throws IOException {
    int importedFiles = 0;
    int skippedFiles = 0;
    long importedSize = 0;
    Map<String, Path> directories = collectDirectories();

    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
      var entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) continue;
        String entryName = entry.getName();
        // 跳过清单文件
        if ("MANIFEST.json".equals(entryName)) continue;

        String[] parts = entryName.split("/", 2);
        if (parts.length != 2) {
          log.warn("跳过无法识别的 ZIP 条目: {}", entryName);
          skippedFiles++;
          continue;
        }
        String dirKey = parts[0];
        String relativePath = parts[1];

        Path targetDir = directories.get(dirKey);
        if (targetDir == null) {
          log.warn("跳过未知存储目录的 ZIP 条目: {} -> {}", dirKey, entryName);
          skippedFiles++;
          continue;
        }

        // 安全检查：防止路径穿越
        Path target = targetDir.resolve(relativePath).normalize();
        if (!target.startsWith(targetDir)) {
          log.warn("跳过路径穿越风险的 ZIP 条目: {}", entryName);
          skippedFiles++;
          continue;
        }

        if (Files.exists(target) && !overwrite) {
          skippedFiles++;
          continue;
        }

        Files.createDirectories(target.getParent());
        try (InputStream is = zipFile.getInputStream(entry)) {
          Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        importedFiles++;
        importedSize += entry.getSize();
      }
    } finally {
      // 清理临时 ZIP 文件
      try {
        Files.deleteIfExists(zipPath);
      } catch (IOException e) {
        log.warn("删除临时 ZIP 文件失败: {}", e.getMessage());
      }
    }
    log.info("文件资源导入完成: 导入 {} 个文件, 跳过 {} 个, 总大小 {} 字节", importedFiles, skippedFiles, importedSize);
    return new ImportResult(importedFiles, skippedFiles, importedSize);
  }

  private Map<String, Path> collectDirectories() {
    Map<String, Path> map = new LinkedHashMap<>();
    map.put("avatars", Paths.get(avatarStorageDir).toAbsolutePath().normalize());
    map.put("images", Paths.get(imageStorageDir).toAbsolutePath().normalize());
    map.put("leave-attachments", Paths.get(leaveAttachmentStorageDir).toAbsolutePath().normalize());
    map.put("document-templates", Paths.get(documentTemplateStorageDir).toAbsolutePath().normalize());
    map.put("generated-documents", Paths.get(generatedDocumentStorageDir).toAbsolutePath().normalize());
    return map;
  }

  private String resolveDisplayPath(Path dir) {
    Path currentDir = Paths.get(".").toAbsolutePath().normalize();
    if (dir.startsWith(currentDir)) {
      return "." + currentDir.relativize(dir).toString();
    }
    return dir.toString();
  }

  private void addDirectoryToZip(
      ZipOutputStream zos, Path dir, String prefix, FileCounter counter) throws IOException {
    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String relativePath = dir.relativize(file).toString().replace('\\', '/');
            String zipEntryName = prefix + "/" + relativePath;
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zipEntry.setSize(attrs.size());
            zos.putNextEntry(zipEntry);
            // 使用缓冲流读取文件，提高 I/O 效率
            try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(Files.newInputStream(file), 8192)) {
              byte[] buffer = new byte[8192];
              int bytesRead;
              while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
              }
            }
            zos.closeEntry();
            counter.count++;
            counter.size += attrs.size();
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private void writeManifest(
      ZipOutputStream zos, Map<String, Path> directories, long totalFiles, long totalSize)
      throws IOException {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"exportedAt\": \"")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\",\n");
    sb.append("  \"totalFiles\": ").append(totalFiles).append(",\n");
    sb.append("  \"totalSize\": ").append(totalSize).append(",\n");
    sb.append("  \"directories\": [\n");
    boolean first = true;
    for (Map.Entry<String, Path> entry : directories.entrySet()) {
      if (!first) sb.append(",\n");
      first = false;
      sb.append("    {\"name\": \"")
          .append(entry.getKey())
          .append("\", \"path\": \"")
          .append(entry.getValue())
          .append("\"}");
    }
    sb.append("\n  ]\n");
    sb.append("}\n");
    ZipEntry manifestEntry = new ZipEntry("MANIFEST.json");
    zos.putNextEntry(manifestEntry);
    zos.write(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    zos.closeEntry();
  }

  private static class FileCounter {
    long count = 0;
    long size = 0;
  }

  @Getter
  public static class ExportResult {
    private final long totalFiles;
    private final long totalSize;
    private final String exportedAt;

    public ExportResult(long totalFiles, long totalSize) {
      this.totalFiles = totalFiles;
      this.totalSize = totalSize;
      this.exportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
  }

  @Getter
  public static class ImportResult {
    private final int importedFiles;
    private final int skippedFiles;
    private final long importedSize;

    public ImportResult(int importedFiles, int skippedFiles, long importedSize) {
      this.importedFiles = importedFiles;
      this.skippedFiles = skippedFiles;
      this.importedSize = importedSize;
    }
  }

  @Getter
  public static class DirectoryStats {
    private final String name;
    private final String path;
    private final long fileCount;
    private final long totalSize;
    private final boolean exists;

    public DirectoryStats(String name, String path, long fileCount, long totalSize, boolean exists) {
      this.name = name;
      this.path = path;
      this.fileCount = fileCount;
      this.totalSize = totalSize;
      this.exists = exists;
    }
  }
}
