package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jmi.openatom.server.openatomsystem.common.Times;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.entity.ImageHostingAsset;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.ImageHostingAssetMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageHostingAssetVO;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseImageUploadVO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ImageHostingStorageServiceImpl {
  private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
  private static final int MAX_ORIGINAL_NAME_LENGTH = 255;

  private final ImageHostingAssetMapper imageHostingAssetMapper;
  private final UserMapper userMapper;

  @Value("${app.image-hosting.storage-dir:./uploads/images}")
  private String storageDir;

  public ResponseImageUploadVO upload(MultipartFile file, String baseUrl, Integer uploaderId)
      throws IOException {
    StoredImage storedImage = store(file);
    return saveAsset(storedImage, baseUrl, uploaderId);
  }

  public ResponseImageUploadVO uploadDataUrl(
      String dataUrl, String originalName, String baseUrl, Integer uploaderId) throws IOException {
    StoredImage storedImage = storeDataUrl(dataUrl, originalName);
    return saveAsset(storedImage, baseUrl, uploaderId);
  }

  private ResponseImageUploadVO saveAsset(StoredImage storedImage, String baseUrl, Integer uploaderId) {
    Timestamp now = Times.now();
    String url = baseUrl + storedImage.getFileName();
    ImageHostingAsset asset =
        ImageHostingAsset.builder()
            .uploaderId(uploaderId)
            .fileName(storedImage.getFileName())
            .originalName(storedImage.getOriginalName())
            .contentType(storedImage.getMediaType().toString())
            .fileSize(storedImage.getFileSize())
            .url(url)
            .status("active")
            .createdAt(now)
            .build();
    imageHostingAssetMapper.insert(asset);
    return ResponseImageUploadVO.builder()
        .id(asset.getId())
        .fileName(asset.getFileName())
        .originalName(asset.getOriginalName())
        .contentType(asset.getContentType())
        .fileSize(asset.getFileSize())
        .url(asset.getUrl())
        .markdown(markdown(asset.getUrl()))
        .createdAt(asset.getCreatedAt())
        .build();
  }

  public PageDataVO<ResponseImageHostingAssetVO> myImages(
      Integer uploaderId, String keyword, Long page, Long pageSize) {
    Page<ImageHostingAsset> imagePage =
        imageHostingAssetMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            uploaderId,
            keyword,
            null,
            true);
    return toPage(imagePage);
  }

  public PageDataVO<ResponseImageHostingAssetVO> adminImages(
      String keyword, String status, Long page, Long pageSize) {
    Page<ImageHostingAsset> imagePage =
        imageHostingAssetMapper.selectPageByConditions(
            new Page<>(PageRequests.page(page), PageRequests.pageSize(pageSize)),
            null,
            keyword,
            status,
            false);
    return toPage(imagePage);
  }

  public boolean deleteOwnImage(Integer imageId, Integer userId) throws IOException {
    ImageHostingAsset asset = imageId == null ? null : imageHostingAssetMapper.selectById(imageId);
    if (asset == null || !"active".equals(asset.getStatus())) return false;
    if (!Objects.equals(asset.getUploaderId(), userId)) return false;
    return markDeleted(asset, userId);
  }

  public boolean adminDeleteImage(Integer imageId, Integer userId) throws IOException {
    ImageHostingAsset asset = imageId == null ? null : imageHostingAssetMapper.selectById(imageId);
    if (asset == null || !"active".equals(asset.getStatus())) return false;
    return markDeleted(asset, userId);
  }

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
    return new StoredImage(
        fileName,
        normalizeOriginalName(file.getOriginalFilename()),
        file.getSize(),
        imageType.mediaType());
  }

  private StoredImage storeDataUrl(String dataUrl, String originalName) throws IOException {
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
    return new StoredImage(
        fileName,
        normalizeOriginalName(originalName),
        (long) data.length,
        imageType.mediaType());
  }

  public Optional<ImageResource> load(String fileName) throws IOException {
    if (!isSafeFileName(fileName)) return Optional.empty();
    ImageHostingAsset asset = imageHostingAssetMapper.selectByFileName(fileName);
    if (asset != null && !"active".equals(asset.getStatus())) return Optional.empty();
    Path root = root();
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root) || !Files.exists(target) || !Files.isRegularFile(target)) {
      return Optional.empty();
    }
    Resource resource = new UrlResource(target.toUri());
    if (!resource.exists() || !resource.isReadable()) return Optional.empty();
    return Optional.of(new ImageResource(resource, mediaTypeOf(fileName)));
  }

  private boolean markDeleted(ImageHostingAsset asset, Integer userId) throws IOException {
    asset.setStatus("deleted");
    asset.setDeletedBy(userId);
    asset.setDeletedAt(Times.now());
    int row = imageHostingAssetMapper.updateById(asset);
    if (row > 0) {
      deletePhysical(asset.getFileName());
      return true;
    }
    return false;
  }

  private void deletePhysical(String fileName) throws IOException {
    if (!isSafeFileName(fileName)) return;
    Path root = root();
    Path target = root.resolve(fileName).normalize();
    if (target.getParent().equals(root)) {
      Files.deleteIfExists(target);
    }
  }

  private PageDataVO<ResponseImageHostingAssetVO> toPage(Page<ImageHostingAsset> page) {
    return PageDataVO.<ResponseImageHostingAssetVO>builder()
        .list(toResponseList(page.getRecords()))
        .page(page.getCurrent())
        .pageSize(page.getSize())
        .total(page.getTotal())
        .build();
  }

  private List<ResponseImageHostingAssetVO> toResponseList(List<ImageHostingAsset> assets) {
    if (assets == null || assets.isEmpty()) return List.of();
    Map<Integer, User> users = userMap(assets);
    return assets.stream().map(asset -> toResponse(asset, users.get(asset.getUploaderId()))).toList();
  }

  private Map<Integer, User> userMap(List<ImageHostingAsset> assets) {
    List<Integer> userIds =
        assets.stream()
            .map(ImageHostingAsset::getUploaderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (userIds.isEmpty()) return Map.of();
    return userMapper.selectBatchIds(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
  }

  private ResponseImageHostingAssetVO toResponse(ImageHostingAsset asset, User uploader) {
    return ResponseImageHostingAssetVO.builder()
        .id(asset.getId())
        .uploaderId(asset.getUploaderId())
        .uploaderName(displayName(uploader))
        .uploaderAvatar(uploader == null ? null : uploader.getAvatar())
        .fileName(asset.getFileName())
        .originalName(asset.getOriginalName())
        .contentType(asset.getContentType())
        .fileSize(asset.getFileSize())
        .url(asset.getUrl())
        .markdown(markdown(asset.getUrl()))
        .status(asset.getStatus())
        .deletedAt(asset.getDeletedAt())
        .createdAt(asset.getCreatedAt())
        .build();
  }

  private String markdown(String url) {
    return "![图片](" + url + ")";
  }

  private String displayName(User user) {
    if (user == null) return null;
    String realName = trimToNull(user.getRealName());
    return realName == null ? user.getUserName() : realName;
  }

  private String normalizeOriginalName(String fileName) {
    String value = trimToNull(fileName);
    if (value == null) return "image";
    value = Paths.get(value).getFileName().toString();
    if (value.length() <= MAX_ORIGINAL_NAME_LENGTH) return value;
    return value.substring(0, MAX_ORIGINAL_NAME_LENGTH);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
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
    private final String originalName;
    private final Long fileSize;
    private final MediaType mediaType;

    public StoredImage(String fileName, String originalName, Long fileSize, MediaType mediaType) {
      this.fileName = fileName;
      this.originalName = originalName;
      this.fileSize = fileSize;
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
