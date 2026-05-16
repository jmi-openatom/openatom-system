package edu.jmi.openatom.server.openatomsystem.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stores and loads user avatar images from local disk. */
@Service
public class AvatarStorageService {
  private static final long MAX_AVATAR_SIZE_BYTES = 2L * 1024 * 1024;

  @Value("${app.avatar.storage-dir:./uploads/avatars}")
  private String storageDir;

  public StoredAvatar store(MultipartFile file) throws IOException {
    validate(file);
    String extension = extensionOf(file.getContentType());
    Path root = root();
    Files.createDirectories(root);
    String fileName = UUID.randomUUID() + extension;
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root)) throw new IOException("非法头像文件名");
    try (InputStream input = file.getInputStream()) {
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return new StoredAvatar(fileName);
  }

  public Optional<AvatarResource> load(String fileName) throws IOException {
    if (!isSafeFileName(fileName)) return Optional.empty();
    Path root = root();
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root) || !Files.exists(target) || !Files.isRegularFile(target)) {
      return Optional.empty();
    }
    Resource resource = new UrlResource(target.toUri());
    if (!resource.exists() || !resource.isReadable()) return Optional.empty();
    return Optional.of(new AvatarResource(resource, mediaTypeOf(fileName)));
  }

  public void deleteByAvatarUrl(String avatarUrl) {
    if (avatarUrl == null || avatarUrl.isBlank()) return;
    String fileName = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
    if (!isSafeFileName(fileName)) return;
    try {
      Files.deleteIfExists(root().resolve(fileName).normalize());
    } catch (IOException ignored) {
      // Best-effort cleanup; avatar replacement must not fail if an old file cannot be removed.
    }
  }

  private void validate(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) throw new IOException("请选择头像图片");
    if (file.getSize() > MAX_AVATAR_SIZE_BYTES) throw new IOException("头像图片不能超过 2MB");
    String contentType = file.getContentType();
    if (!MediaType.IMAGE_JPEG_VALUE.equals(contentType) && !MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
      throw new IOException("仅支持 JPG 或 PNG 头像");
    }
    try (InputStream input = file.getInputStream()) {
      if (ImageIO.read(input) == null) throw new IOException("头像文件不是有效图片");
    }
  }

  private Path root() {
    return Paths.get(storageDir).toAbsolutePath().normalize();
  }

  private String extensionOf(String contentType) {
    return MediaType.IMAGE_PNG_VALUE.equals(contentType) ? ".png" : ".jpg";
  }

  private MediaType mediaTypeOf(String fileName) {
    return fileName.toLowerCase(Locale.ROOT).endsWith(".png")
        ? MediaType.IMAGE_PNG
        : MediaType.IMAGE_JPEG;
  }

  private boolean isSafeFileName(String fileName) {
    return fileName != null && fileName.matches("^[a-f0-9-]{36}\\.(png|jpg)$");
  }

  @Getter
  public static class StoredAvatar {
    private final String fileName;

    public StoredAvatar(String fileName) {
      this.fileName = fileName;
    }
  }

  @Getter
  public static class AvatarResource {
    private final Resource resource;
    private final MediaType mediaType;

    public AvatarResource(Resource resource, MediaType mediaType) {
      this.resource = resource;
      this.mediaType = mediaType;
    }
  }
}
