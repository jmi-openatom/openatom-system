package edu.jmi.openatom.server.openatomsystem.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/** 请假附件私有存储服务。 */
@Service
public class LeaveAttachmentStorageServiceImpl {
  private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
  private static final int MAX_ORIGINAL_NAME_LENGTH = 255;

  @Value("${app.leave-attachment.storage-dir:./uploads/leave-attachments}")
  private String storageDir;

  @Value("${app.image-hosting.storage-dir:./uploads/images}")
  private String legacyImageStorageDir;

  public StoredLeaveAttachment uploadDataUrl(String dataUrl, String originalName) throws IOException {
    String value = trimToNull(dataUrl);
    if (value == null || !value.startsWith("data:image/")) {
      throw new IOException("图片内容不是 data:image 地址");
    }
    int commaIndex = value.indexOf(',');
    if (commaIndex < 0) {
      throw new IOException("图片 data 地址格式不正确");
    }
    String metadata = value.substring(5, commaIndex).toLowerCase(Locale.ROOT);
    if (!metadata.startsWith("image/") || !metadata.contains(";base64")) {
      throw new IOException("图片 data 地址必须是 base64 图片");
    }
    byte[] data;
    try {
      data = Base64.getDecoder().decode(value.substring(commaIndex + 1).trim());
    } catch (IllegalArgumentException e) {
      throw new IOException("图片 base64 内容不正确");
    }
    if (data.length == 0) throw new IOException("图片内容为空");
    if (data.length > MAX_IMAGE_SIZE_BYTES) throw new IOException("图片不能超过 10MB");
    ImageType imageType = ImageType.detect(data);
    if (imageType == null) {
      throw new IOException("仅支持 JPG、PNG、GIF 或 WebP 图片");
    }
    Path root = root();
    Files.createDirectories(root);
    String fileName = UUID.randomUUID() + imageType.extension();
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root)) throw new IOException("非法图片文件名");
    try (InputStream input = new ByteArrayInputStream(data)) {
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return new StoredLeaveAttachment(
        fileName,
        normalizeOriginalName(originalName),
        (long) data.length,
        imageType.mediaType());
  }

  public Optional<LeaveAttachmentResource> load(String fileName) throws IOException {
    if (!isSafeFileName(fileName)) return Optional.empty();
    Optional<LeaveAttachmentResource> resource = loadFrom(root(), fileName);
    if (resource.isPresent()) return resource;
    return loadFrom(legacyRoot(), fileName);
  }

  private Optional<LeaveAttachmentResource> loadFrom(Path root, String fileName) throws IOException {
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root) || !Files.exists(target) || !Files.isRegularFile(target)) {
      return Optional.empty();
    }
    Resource resource = new UrlResource(target.toUri());
    if (!resource.exists() || !resource.isReadable()) return Optional.empty();
    return Optional.of(new LeaveAttachmentResource(resource, mediaTypeOf(fileName)));
  }

  private Path root() {
    return Paths.get(storageDir).toAbsolutePath().normalize();
  }

  private Path legacyRoot() {
    return Paths.get(legacyImageStorageDir).toAbsolutePath().normalize();
  }

  private String normalizeOriginalName(String fileName) {
    String value = trimToNull(fileName);
    if (value == null) return "leave-attachment";
    value = Paths.get(value).getFileName().toString();
    if (value.length() <= MAX_ORIGINAL_NAME_LENGTH) return value;
    return value.substring(0, MAX_ORIGINAL_NAME_LENGTH);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private MediaType mediaTypeOf(String fileName) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
    if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
    if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
    return MediaType.IMAGE_JPEG;
  }

  private boolean isSafeFileName(String fileName) {
    return fileName != null && fileName.matches("^[a-f0-9-]{36}\\.(png|jpg|gif|webp)$");
  }

  private enum ImageType {
    JPEG(".jpg", MediaType.IMAGE_JPEG),
    PNG(".png", MediaType.IMAGE_PNG),
    GIF(".gif", MediaType.IMAGE_GIF),
    WEBP(".webp", MediaType.parseMediaType("image/webp"));

    private final String extension;
    private final MediaType mediaType;

    ImageType(String extension, MediaType mediaType) {
      this.extension = extension;
      this.mediaType = mediaType;
    }

    String extension() {
      return extension;
    }

    MediaType mediaType() {
      return mediaType;
    }

    static ImageType detect(byte[] header) {
      if (startsWith(header, 0xFF, 0xD8, 0xFF)) return JPEG;
      if (startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return PNG;
      if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) return GIF;
      if (startsWithAscii(header, "RIFF") && header.length >= 12 && asciiAt(header, 8, "WEBP")) {
        return WEBP;
      }
      return null;
    }

    private static boolean startsWith(byte[] value, int... expected) {
      if (value == null || value.length < expected.length) return false;
      for (int i = 0; i < expected.length; i++) {
        if ((value[i] & 0xff) != expected[i]) return false;
      }
      return true;
    }

    private static boolean startsWithAscii(byte[] value, String expected) {
      return asciiAt(value, 0, expected);
    }

    private static boolean asciiAt(byte[] value, int offset, String expected) {
      if (value == null || value.length < offset + expected.length()) return false;
      for (int i = 0; i < expected.length(); i++) {
        if (value[offset + i] != (byte) expected.charAt(i)) return false;
      }
      return true;
    }
  }

  @Getter
  public static class StoredLeaveAttachment {
    private final String fileName;
    private final String originalName;
    private final Long fileSize;
    private final MediaType mediaType;

    public StoredLeaveAttachment(String fileName, String originalName, Long fileSize, MediaType mediaType) {
      this.fileName = fileName;
      this.originalName = originalName;
      this.fileSize = fileSize;
      this.mediaType = mediaType;
    }
  }

  @Getter
  public static class LeaveAttachmentResource {
    private final Resource resource;
    private final MediaType mediaType;

    public LeaveAttachmentResource(Resource resource, MediaType mediaType) {
      this.resource = resource;
      this.mediaType = mediaType;
    }
  }
}
