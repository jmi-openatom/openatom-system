package edu.jmi.openatom.server.openatomsystem.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Private storage: photos are only publicly readable after their submission is approved. */
@Service
public class CampusBuildingPhotoStorage {
  public static final long MAX_BYTES = 10L * 1024 * 1024;

  @Value("${app.campus-building.storage-dir:./uploads/campus-buildings}")
  private String storageDir;

  public void validate(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
      throw new IllegalArgumentException("实拍图不能为空且每张不能超过 10MB");
    }
    type(file);
  }

  public StoredPhoto store(MultipartFile file) throws IOException {
    validate(file);
    String mimeType = type(file);
    String extension = switch (mimeType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      default -> ".webp";
    };
    Files.createDirectories(root());
    String name = UUID.randomUUID() + extension;
    Path target = root().resolve(name);
    try (InputStream stream = file.getInputStream()) {
      Files.copy(stream, target);
    } catch (IOException e) {
      Files.deleteIfExists(target);
      throw e;
    }
    String original = file.getOriginalFilename();
    original = original == null ? "校园实拍" + extension : original.replace('\\', '/');
    original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "").trim();
    if (original.isBlank()) original = "校园实拍" + extension;
    if (original.length() > 255) original = original.substring(0, 255);
    return new StoredPhoto(name, original, mimeType, file.getSize());
  }

  public Resource load(String name) throws IOException {
    Path target = safePath(name);
    if (!Files.isRegularFile(target)) throw new IOException("实拍图不存在");
    return new UrlResource(target.toUri());
  }

  public void delete(String name) throws IOException {
    Files.deleteIfExists(safePath(name));
  }

  private Path safePath(String name) throws IOException {
    if (name == null || !name.matches("^[a-f0-9-]{36}\\.(jpg|png|webp)$")) {
      throw new IOException("非法实拍图文件名");
    }
    Path path = root().resolve(name).normalize();
    if (!path.getParent().equals(root())) throw new IOException("非法实拍图路径");
    return path;
  }

  private Path root() {
    return Paths.get(storageDir).toAbsolutePath().normalize();
  }

  private String type(MultipartFile file) throws IOException {
    byte[] bytes;
    try (InputStream stream = file.getInputStream()) {
      bytes = stream.readNBytes(12);
    }
    if (starts(bytes, new int[] {0xff, 0xd8, 0xff})) return "image/jpeg";
    if (starts(bytes, new int[] {0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return "image/png";
    if (starts(bytes, new int[] {'R', 'I', 'F', 'F'}) && bytes.length >= 12
        && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
    throw new IllegalArgumentException("实拍图仅支持 JPG、PNG、WebP");
  }

  private boolean starts(byte[] bytes, int[] signature) {
    if (bytes.length < signature.length) return false;
    for (int i = 0; i < signature.length; i++) {
      if ((bytes[i] & 0xff) != signature[i]) return false;
    }
    return true;
  }

  public record StoredPhoto(String storageName, String originalName, String mimeType, long size) {}
}
