package edu.jmi.openatom.server.openatomsystem.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图床图片存储服务
 *
 * <p>将博客正文图片保存到独立目录，并通过 /files/images 暴露公开读取地址
 */
@Service
public class ImageHostingStorageServiceImpl {
  private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

  @Value("${app.image-hosting.storage-dir:./uploads/images}")
  private String storageDir;

  public StoredImage store(MultipartFile file) throws IOException {
    ImageType imageType = validate(file);
    Path root = root();
    Files.createDirectories(root);
    String fileName = UUID.randomUUID() + imageType.extension();
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root)) throw new IOException("非法图片文件名");
    try (InputStream input = file.getInputStream()) {
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return new StoredImage(fileName, imageType.mediaType());
  }

  public Optional<ImageResource> load(String fileName) throws IOException {
    if (!isSafeFileName(fileName)) return Optional.empty();
    Path root = root();
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root) || !Files.exists(target) || !Files.isRegularFile(target)) {
      return Optional.empty();
    }
    Resource resource = new UrlResource(target.toUri());
    if (!resource.exists() || !resource.isReadable()) return Optional.empty();
    return Optional.of(new ImageResource(resource, mediaTypeOf(fileName)));
  }

  private ImageType validate(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) throw new IOException("请选择图片文件");
    if (file.getSize() > MAX_IMAGE_SIZE_BYTES) throw new IOException("图片不能超过 10MB");

    byte[] header;
    try (InputStream input = file.getInputStream()) {
      header = input.readNBytes(16);
    }
    ImageType imageType = ImageType.detect(header);
    if (imageType == null) {
      throw new IOException("仅支持 JPG、PNG、GIF 或 WebP 图片");
    }
    return imageType;
  }

  private Path root() {
    return Paths.get(storageDir).toAbsolutePath().normalize();
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
  public static class StoredImage {
    private final String fileName;
    private final MediaType mediaType;

    public StoredImage(String fileName, MediaType mediaType) {
      this.fileName = fileName;
      this.mediaType = mediaType;
    }
  }

  @Getter
  public static class ImageResource {
    private final Resource resource;
    private final MediaType mediaType;

    public ImageResource(Resource resource, MediaType mediaType) {
      this.resource = resource;
      this.mediaType = mediaType;
    }
  }
}
