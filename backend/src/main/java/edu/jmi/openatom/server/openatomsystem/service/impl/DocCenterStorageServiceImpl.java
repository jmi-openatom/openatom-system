package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.entity.DocCenterDocument;
import edu.jmi.openatom.server.openatomsystem.service.DocCenterStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocCenterStorageServiceImpl implements DocCenterStorageService {
  private static final long MAX_SIZE = 50L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS =
      Set.of("docx", "xlsx", "pptx");

  @Value("${app.document-center.storage-dir:./uploads/document-center}")
  private String storageDir;

  @Override
  public DocCenterDocument store(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("document_empty");
    }
    if (file.getSize() > MAX_SIZE) {
      throw new IllegalArgumentException("document_too_large");
    }
    String original = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
    String extension = extensionOf(original);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException("document_type_not_allowed");
    }
    String storageName = UUID.randomUUID() + "." + extension;
    Path target = root().resolve(storageName).normalize();
    if (!target.getParent().equals(root())) {
      throw new IOException("invalid_document_name");
    }
    Files.createDirectories(root());
    try (InputStream input = file.getInputStream()) {
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return DocCenterDocument.builder()
        .name(original)
        .extension(extension)
        .sizeBytes(file.getSize())
        .storageName(storageName)
        .build();
  }

  @Override
  public void overwrite(DocCenterDocument document, byte[] content) throws IOException {
    if (document.getStorageName() == null || document.getStorageName().isBlank()) {
      throw new IOException("missing_storage_name");
    }
    Path target = root().resolve(document.getStorageName()).normalize();
    if (!target.getParent().equals(root())) {
      throw new IOException("invalid_document_name");
    }
    Files.createDirectories(root());
    Files.write(target, content);
  }

  @Override
  public Optional<Resource> load(DocCenterDocument document) {
    if (document.getStorageName() == null || document.getStorageName().isBlank()) {
      return Optional.empty();
    }
    Path target = root().resolve(document.getStorageName()).normalize();
    if (!target.getParent().equals(root()) || !Files.isRegularFile(target)) {
      return Optional.empty();
    }
    return Optional.of(new FileSystemResource(target));
  }

  @Override
  public void delete(DocCenterDocument document) {
    if (document.getStorageName() == null || document.getStorageName().isBlank()) {
      return;
    }
    Path target = root().resolve(document.getStorageName()).normalize();
    if (target.getParent().equals(root())) {
      try {
        Files.deleteIfExists(target);
      } catch (IOException ignored) {
        // 磁盘清理失败不影响记录删除
      }
    }
  }

  private Path root() {
    return Path.of(storageDir.trim()).toAbsolutePath().normalize();
  }

  private String extensionOf(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }
}
