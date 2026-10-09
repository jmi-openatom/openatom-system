package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampusBuildingService {
  public static final int MAX_PHOTOS = 9;
  private final NamedParameterJdbcTemplate jdbc;
  private final CampusBuildingCatalog catalog;
  private final CampusBuildingPhotoStorage storage;
  private static final String VISIBLE_PHOTO = """
      AND NOT EXISTS (SELECT 1 FROM campus_building_photo_removal removal
        JOIN campus_building_submission editor ON editor.id = removal.submission_id
        WHERE removal.photo_id = p.id AND editor.status = 'approved')
      """;

  public BuildingDetail publicDetail(String buildingId, String baseUrl) {
    String name = catalog.name(buildingId);
    var params = new MapSqlParameterSource("buildingId", buildingId);
    List<String> descriptions = jdbc.query("""
        SELECT description FROM campus_building_submission
        WHERE building_id = :buildingId AND status = 'approved' AND replace_description = 1
        ORDER BY reviewed_at DESC, id DESC LIMIT 1
        """, params, (rs, index) -> rs.getString("description"));
    List<Photo> photos = jdbc.query("""
        SELECT p.id, p.original_name, p.submission_id FROM campus_building_photo p
        JOIN campus_building_submission s ON s.id = p.submission_id
        WHERE s.building_id = :buildingId AND s.status = 'approved'
        """ + VISIBLE_PHOTO + " ORDER BY s.reviewed_at DESC, s.id DESC, p.sort_order, p.id", params, photoMapper(baseUrl, true));
    return new BuildingDetail(buildingId, name, descriptions.isEmpty() ? catalog.description(buildingId) : descriptions.get(0), photos);
  }

  @Transactional(rollbackFor = Exception.class)
  public long submit(String buildingId, String description, boolean replaceDescription,
      List<MultipartFile> files, List<Long> removedPhotoIds, Integer userId)
      throws IOException {
    catalog.name(buildingId);
    String text = description == null ? "" : description.trim();
    List<MultipartFile> photos = files == null ? List.of() : files;
    List<Long> removals = removedPhotoIds == null ? List.of() : removedPhotoIds.stream().distinct().toList();
    if (userId == null) throw new IllegalArgumentException("请先登录");
    if (text.length() > 4000) throw new IllegalArgumentException("介绍不能超过 4000 字");
    if (photos.size() > MAX_PHOTOS) throw new IllegalArgumentException("每次最多提交 9 张实拍图");
    if (!replaceDescription && photos.isEmpty() && removals.isEmpty()) throw new IllegalArgumentException("请修改介绍或实拍图");
    if (!replaceDescription && !text.isBlank()) throw new IllegalArgumentException("请勾选修改介绍");
    if (removals.size() > 100 || removals.stream().anyMatch(id -> id == null || id < 1)) {
      throw new IllegalArgumentException("无效的移除照片列表");
    }
    for (MultipartFile photo : photos) storage.validate(photo);
    // A contributor can have one pending submission per building. Serialize on the user row
    // so simultaneous multipart requests cannot bypass this check.
    jdbc.queryForList("SELECT id FROM tb_user WHERE id = :userId FOR UPDATE", Map.of("userId", userId));
    Long pending = jdbc.queryForObject("""
        SELECT COUNT(*) FROM campus_building_submission
        WHERE building_id = :buildingId AND user_id = :userId AND status = 'pending'
        """, Map.of("buildingId", buildingId, "userId", userId), Long.class);
    if (pending != null && pending > 0) throw new IllegalArgumentException("这栋楼已有你的待审核提交，请等待审核后再提交");
    if (!removals.isEmpty()) {
      Long count = jdbc.queryForObject("""
          SELECT COUNT(*) FROM campus_building_photo p
          JOIN campus_building_submission s ON s.id = p.submission_id
          WHERE s.building_id = :buildingId AND s.status = 'approved' AND p.id IN (:ids)
          """ + VISIBLE_PHOTO, new MapSqlParameterSource("buildingId", buildingId).addValue("ids", removals), Long.class);
      if (count == null || count != removals.size()) throw new IllegalArgumentException("待移除照片不属于这栋楼或已被修改，请刷新后重试");
    }
    var key = new GeneratedKeyHolder();
    jdbc.update("""
        INSERT INTO campus_building_submission (building_id, user_id, description, replace_description, status)
        VALUES (:buildingId, :userId, :description, :replaceDescription, 'pending')
        """, new MapSqlParameterSource("buildingId", buildingId).addValue("userId", userId)
            .addValue("description", text.isBlank() ? null : text).addValue("replaceDescription", replaceDescription), key, new String[] {"id"});
    long id = key.getKey().longValue();
    for (Long photoId : removals) {
      jdbc.update("INSERT INTO campus_building_photo_removal (submission_id, photo_id) VALUES (:id, :photoId)",
          Map.of("id", id, "photoId", photoId));
    }
    List<String> storedNames = new ArrayList<>();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
          if (status != STATUS_COMMITTED) cleanup(storedNames);
        }
      });
    }
    try {
      for (int i = 0; i < photos.size(); i++) {
        var photo = storage.store(photos.get(i));
        storedNames.add(photo.storageName());
        jdbc.update("""
            INSERT INTO campus_building_photo
              (submission_id, storage_name, original_name, mime_type, file_size, sort_order)
            VALUES (:submissionId, :storageName, :originalName, :mimeType, :size, :sortOrder)
            """, new MapSqlParameterSource("submissionId", id)
                .addValue("storageName", photo.storageName()).addValue("originalName", photo.originalName())
                .addValue("mimeType", photo.mimeType()).addValue("size", photo.size()).addValue("sortOrder", i));
      }
    } catch (IOException | RuntimeException error) {
      cleanup(storedNames);
      throw error;
    }
    return id;
  }

  public PageDataVO<Submission> submissions(String buildingId, Integer userId, String status,
      Long page, Long pageSize, String baseUrl) {
    if (buildingId != null && !buildingId.isBlank()) catalog.name(buildingId);
    if (status != null && !List.of("pending", "approved", "rejected", "hidden").contains(status)) {
      throw new IllegalArgumentException("无效的审核状态");
    }
    var params = new MapSqlParameterSource();
    String where = " WHERE 1 = 1";
    if (buildingId != null && !buildingId.isBlank()) {
      where += " AND s.building_id = :buildingId";
      params.addValue("buildingId", buildingId);
    }
    if (userId != null) {
      where += " AND s.user_id = :userId";
      params.addValue("userId", userId);
    }
    if (status != null) {
      where += " AND s.status = :status";
      params.addValue("status", status);
    }
    Long total = jdbc.queryForObject("SELECT COUNT(*) FROM campus_building_submission s" + where, params, Long.class);
    long current = PageRequests.page(page), size = PageRequests.pageSize(pageSize);
    params.addValue("offset", (current - 1) * size).addValue("limit", size);
    List<Submission> rows = jdbc.query("""
        SELECT s.*, COALESCE(NULLIF(u.real_name, ''), u.user_name, '用户') AS author_name
        FROM campus_building_submission s LEFT JOIN tb_user u ON u.id = s.user_id
        """ + where + " ORDER BY s.created_at DESC, s.id DESC LIMIT :limit OFFSET :offset", params, (rs, index) ->
        new Submission(rs.getLong("id"), rs.getString("building_id"), catalog.name(rs.getString("building_id")),
            rs.getInt("user_id"), rs.getString("author_name"), rs.getString("description"), rs.getBoolean("replace_description"), rs.getString("status"),
            rs.getString("review_reason"), rs.getTimestamp("created_at"), rs.getTimestamp("reviewed_at"), List.of(), List.of()));
    if (!rows.isEmpty()) {
      List<Photo> photos = jdbc.query("""
          SELECT id, submission_id, original_name FROM campus_building_photo
          WHERE submission_id IN (:ids) ORDER BY sort_order, id
          """, Map.of("ids", rows.stream().map(Submission::id).toList()), photoMapper(baseUrl, false));
      var grouped = photos.stream().collect(Collectors.groupingBy(Photo::submissionId));
      List<Photo> removals = jdbc.query("""
          SELECT p.id, r.submission_id, p.original_name FROM campus_building_photo_removal r
          JOIN campus_building_photo p ON p.id = r.photo_id
          WHERE r.submission_id IN (:ids) ORDER BY p.id
          """, Map.of("ids", rows.stream().map(Submission::id).toList()), photoMapper(baseUrl, false));
      var groupedRemovals = removals.stream().collect(Collectors.groupingBy(Photo::submissionId));
      rows = rows.stream().map(s -> new Submission(s.id, s.buildingId, s.buildingName, s.userId, s.authorName,
          s.description, s.replaceDescription, s.status, s.reviewReason, s.createdAt, s.reviewedAt,
          grouped.getOrDefault(s.id, List.of()), groupedRemovals.getOrDefault(s.id, List.of()))).toList();
    }
    return new PageDataVO<>(rows, current, size, total == null ? 0L : total);
  }

  @Transactional(rollbackFor = Exception.class)
  public void review(long id, String action, String reason, int reviewerId) {
    String target = switch (action == null ? "" : action) {
      case "approve" -> "approved";
      case "reject" -> "rejected";
      case "hide" -> "hidden";
      default -> throw new IllegalArgumentException("审核操作必须是通过、驳回或下架");
    };
    String text = reason == null ? "" : reason.trim();
    if (text.length() > 500) throw new IllegalArgumentException("审核意见不能超过 500 字");
    if (!"approved".equals(target) && text.isBlank()) throw new IllegalArgumentException("请填写驳回或下架原因");
    int changed = jdbc.update("""
        UPDATE campus_building_submission
        SET status = :status, review_reason = :reason, reviewed_by = :reviewerId, reviewed_at = CURRENT_TIMESTAMP(3)
        WHERE id = :id AND status = :expectedStatus
        """, new MapSqlParameterSource("id", id).addValue("status", target).addValue("reason", text)
            .addValue("reviewerId", reviewerId).addValue("expectedStatus", "hidden".equals(target) ? "approved" : "pending"));
    if (changed != 1) throw new IllegalArgumentException("提交不存在或状态已变化，请刷新后重试");
  }

  public Optional<PhotoFile> photo(long photoId, Integer userId, boolean reviewer, boolean publicOnly) {
    var params = new MapSqlParameterSource("id", photoId).addValue("userId", userId);
    String access = publicOnly ? " AND s.status = 'approved'" + VISIBLE_PHOTO
        : reviewer ? "" : """
            AND (s.user_id = :userId OR EXISTS (
              SELECT 1 FROM campus_building_photo_removal removal
              JOIN campus_building_submission editor ON editor.id = removal.submission_id
              WHERE removal.photo_id = p.id AND editor.user_id = :userId))
            """;
    List<PhotoFile> result = jdbc.query("""
        SELECT p.storage_name, p.mime_type FROM campus_building_photo p
        JOIN campus_building_submission s ON s.id = p.submission_id WHERE p.id = :id
        """ + access, params, (rs, index) -> new PhotoFile(rs.getString("storage_name"), rs.getString("mime_type")));
    return result.stream().findFirst();
  }

  private RowMapper<Photo> photoMapper(String baseUrl, boolean publicOnly) {
    String prefix = baseUrl + (publicOnly ? "/site/campus-building-photos/" : "/campus-buildings/photos/");
    return (rs, index) -> new Photo(rs.getLong("id"), rs.getLong("submission_id"), prefix + rs.getLong("id"), rs.getString("original_name"));
  }

  private void cleanup(List<String> names) {
    for (String name : names) {
      try { storage.delete(name); }
      catch (IOException e) { log.warn("Failed to clean up campus photo {}", name, e); }
    }
  }

  public record Photo(long id, long submissionId, String url, String originalName) {}
  public record PhotoFile(String storageName, String mimeType) {}
  public record BuildingDetail(String buildingId, String buildingName, String description, List<Photo> photos) {}
  public record Submission(long id, String buildingId, String buildingName, int userId, String authorName,
      String description, boolean replaceDescription, String status, String reviewReason, Timestamp createdAt,
      Timestamp reviewedAt, List<Photo> photos, List<Photo> removedPhotos) {}
}
