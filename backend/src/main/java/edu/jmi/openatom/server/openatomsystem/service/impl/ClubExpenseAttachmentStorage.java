package edu.jmi.openatom.server.openatomsystem.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 支出票据只存在私有目录，经受权限检查的接口读取。 */
@Service
public class ClubExpenseAttachmentStorage {
  private static final long MAX_BYTES = 10L * 1024 * 1024;

  @Value("${app.expense-attachment.storage-dir:./uploads/expense-attachments}")
  private String storageDir;

  public StoredFile store(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
      throw new IllegalArgumentException("凭证不能为空且不能超过10MB");
    }
    byte[] header;
    try (InputStream stream = file.getInputStream()) {
      header = stream.readNBytes(12);
    }
    FileType type = FileType.detect(header);
    if (type == null) throw new IllegalArgumentException("凭证仅支持 JPG、PNG、WebP 或 PDF");
    Path root = root();
    Files.createDirectories(root);
    String storageName = UUID.randomUUID() + type.extension;
    Path target = root.resolve(storageName);
    try (InputStream stream = file.getInputStream()) {
      Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
    }
    String original = file.getOriginalFilename();
    original = original == null ? "凭证" : Paths.get(original).getFileName().toString();
    original = original.replaceAll("[\\r\\n]", "").trim();
    if (original.isBlank()) original = "凭证" + type.extension;
    if (original.length() > 255) original = original.substring(0, 255);
    return new StoredFile(storageName, original, type.mimeType, Files.size(target));
  }

  public Resource load(String storageName) throws IOException {
    if (storageName == null || !storageName.matches("^[a-f0-9-]{36}\\.(jpg|png|webp|pdf)$")) {
      throw new IOException("非法凭证文件名");
    }
    Path target = root().resolve(storageName).normalize();
    if (!target.getParent().equals(root()) || !Files.isRegularFile(target)) {
      throw new IOException("凭证文件不存在");
    }
    Resource resource = new UrlResource(target.toUri());
    if (!resource.isReadable()) throw new IOException("凭证文件不可读取");
    return resource;
  }

  public void delete(String storageName) throws IOException {
    Files.deleteIfExists(root().resolve(storageName));
  }

  private Path root() {
    return Paths.get(storageDir).toAbsolutePath().normalize();
  }

  public record StoredFile(String storageName, String originalName, String mimeType, long size) {}

  private enum FileType {
    JPG(".jpg", "image/jpeg"),
    PNG(".png", "image/png"),
    WEBP(".webp", "image/webp"),
    PDF(".pdf", "application/pdf");

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
      this.extension = extension;
      this.mimeType = mimeType;
    }

    static FileType detect(byte[] bytes) {
      if (starts(bytes, new int[] {0xff, 0xd8, 0xff})) return JPG;
      if (starts(bytes, new int[] {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return PNG;
      if (starts(bytes, new int[] {'%', 'P', 'D', 'F', '-'})) return PDF;
      if (starts(bytes, new int[] {'R', 'I', 'F', 'F'})
          && bytes.length >= 12
          && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return WEBP;
      return null;
    }

    private static boolean starts(byte[] bytes, int[] signature) {
      if (bytes.length < signature.length) return false;
      for (int i = 0; i < signature.length; i++) {
        if ((bytes[i] & 0xff) != signature[i]) return false;
      }
      return true;
    }
  }
}
