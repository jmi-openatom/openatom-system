package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.common.web.PageRequests;
import edu.jmi.openatom.server.openatomsystem.dto.RequestClubExpenseDTO;
import edu.jmi.openatom.server.openatomsystem.vo.ClubExpenseVO;
import edu.jmi.openatom.server.openatomsystem.vo.PageDataVO;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 社团已付款支出台账。所有读写路径都按当前用户所属社团限制。 */
@Service
@RequiredArgsConstructor
public class ClubExpenseService {
  private static final Set<String> CATEGORIES =
      Set.of("materials", "venue", "printing", "gifts", "transport", "catering", "other");
  private static final Set<String> PAYMENT_METHODS =
      Set.of("wechat", "alipay", "card", "cash", "other");
  private static final Map<String, String> CATEGORY_LABELS = Map.of(
      "materials", "活动物资", "venue", "场地设备", "printing", "宣传印刷",
      "gifts", "奖品礼品", "transport", "交通", "catering", "餐饮", "other", "其他");
  private static final Map<String, String> PAYMENT_LABELS = Map.of(
      "wechat", "微信", "alipay", "支付宝", "card", "银行卡", "cash", "现金", "other", "其他");
  private static final String SELECT = """
      SELECT e.*, c.name AS club_name, a.title AS activity_title,
        (SELECT COUNT(*) FROM club_expense_attachment x
         WHERE x.expense_id = e.id AND x.deleted_at IS NULL) AS attachment_count
      FROM club_expense e
      JOIN club c ON c.id = e.club_id
      LEFT JOIN club_activity a ON a.id = e.activity_id
      """;
  private static final RowMapper<ClubExpenseVO> EXPENSE_MAPPER = ClubExpenseService::mapExpense;

  private final NamedParameterJdbcTemplate jdbc;
  private final ClubExpenseAttachmentStorage storage;
  private final ObjectMapper objectMapper;
  private final ClubExpenseAccess access;

  public record Filter(
      Integer clubId,
      LocalDate startDate,
      LocalDate endDate,
      String category,
      Integer activityId,
      String status,
      String keyword) {
    public Filter activeOnly() {
      return new Filter(clubId, startDate, endDate, category, activityId, "active", keyword);
    }
  }

  public record Download(Resource resource, String originalName, String mimeType, long size) {}

  public List<ClubExpenseVO.ClubOption> clubs() {
    List<Integer> scope = access.allowedClubIds();
    if (scope != null && scope.isEmpty()) return List.of();
    String sql = "SELECT id, name FROM club" + (scope == null ? "" : " WHERE id IN (:ids)") + " ORDER BY name";
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (scope != null) params.addValue("ids", scope);
    return jdbc.query(sql, params, (rs, rowNum) -> new ClubExpenseVO.ClubOption(rs.getInt("id"), rs.getString("name")));
  }

  public List<ClubExpenseVO.ActivityOption> activities(Integer clubId) {
    access.checkClub(clubId);
    return jdbc.query("SELECT id, title FROM club_activity WHERE club_id = :clubId ORDER BY activity_at DESC, id DESC",
        new MapSqlParameterSource("clubId", clubId),
        (rs, n) -> new ClubExpenseVO.ActivityOption(rs.getInt("id"), rs.getString("title")));
  }

  public PageDataVO<ClubExpenseVO> list(Filter filter, Long page, Long pageSize) {
    Query query = query(filter);
    long safePage = PageRequests.page(page);
    long safeSize = PageRequests.pageSize(pageSize);
    Long total = jdbc.queryForObject("SELECT COUNT(*) FROM club_expense e" + query.where, query.params, Long.class);
    query.params.addValue("limit", safeSize).addValue("offset", (safePage - 1) * safeSize);
    List<ClubExpenseVO> rows = jdbc.query(
        SELECT + query.where + " ORDER BY e.paid_on DESC, e.id DESC LIMIT :limit OFFSET :offset",
        query.params, EXPENSE_MAPPER);
    return new PageDataVO<>(rows, safePage, safeSize, total == null ? 0L : total);
  }

  public ClubExpenseVO.Summary summary(Filter filter) {
    Query query = query(filter.activeOnly());
    Map<String, Object> overall = jdbc.queryForMap(
        "SELECT COUNT(*) AS total_count, COALESCE(SUM(e.amount), 0) AS total_amount FROM club_expense e" + query.where,
        query.params);
    List<ClubExpenseVO.CategoryTotal> byCategory = jdbc.query(
        "SELECT e.category, COUNT(*) AS total_count, SUM(e.amount) AS total_amount FROM club_expense e"
            + query.where + " GROUP BY e.category ORDER BY total_amount DESC",
        query.params,
        (rs, n) -> new ClubExpenseVO.CategoryTotal(rs.getString("category"), rs.getLong("total_count"), rs.getBigDecimal("total_amount")));
    List<ClubExpenseVO.MonthTotal> byMonth = jdbc.query(
        "SELECT DATE_FORMAT(e.paid_on, '%Y-%m') AS month, COUNT(*) AS total_count, SUM(e.amount) AS total_amount FROM club_expense e"
            + query.where + " GROUP BY DATE_FORMAT(e.paid_on, '%Y-%m') ORDER BY month DESC",
        query.params,
        (rs, n) -> new ClubExpenseVO.MonthTotal(rs.getString("month"), rs.getLong("total_count"), rs.getBigDecimal("total_amount")));
    return new ClubExpenseVO.Summary(((Number) overall.get("total_count")).longValue(),
        (BigDecimal) overall.get("total_amount"), byCategory, byMonth);
  }

  public byte[] exportCsv(Filter filter) {
    Query query = query(filter);
    StringBuilder csv = new StringBuilder("\uFEFF付款日期,社团,支出事项,分类,金额,经手人,支付方式,关联活动,备注,状态,作废原因\r\n");
    jdbc.query(SELECT + query.where + " ORDER BY e.paid_on DESC, e.id DESC", query.params,
        rs -> {
          String paymentMethod = rs.getString("payment_method");
          String[] fields = {
              rs.getDate("paid_on").toString(), rs.getString("club_name"), rs.getString("title"),
              CATEGORY_LABELS.getOrDefault(rs.getString("category"), rs.getString("category")),
              rs.getBigDecimal("amount").toPlainString(), rs.getString("handled_by"),
              paymentMethod == null ? "" : PAYMENT_LABELS.getOrDefault(paymentMethod, paymentMethod),
              rs.getString("activity_title"), rs.getString("note"), rs.getString("status"),
              rs.getString("void_reason")};
          for (int i = 0; i < fields.length; i++) {
            if (i > 0) csv.append(',');
            csv.append(csvCell(fields[i]));
          }
          csv.append("\r\n");
        });
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  public ClubExpenseVO detail(Long id) {
    ClubExpenseVO expense = find(id);
    access.checkClub(expense.clubId());
    return withAttachments(expense);
  }

  public List<ClubExpenseVO.History> history(Long id) {
    ClubExpenseVO expense = find(id);
    access.checkClub(expense.clubId());
    return jdbc.query("""
        SELECT h.id, h.action, h.before_json, h.after_json, h.reason, h.operator_id,
          COALESCE(u.real_name, u.user_name) AS operator_name, h.created_at
        FROM club_expense_history h
        LEFT JOIN tb_user u ON u.id = h.operator_id
        WHERE h.expense_id = :id ORDER BY h.id DESC
        """, new MapSqlParameterSource("id", id), (rs, n) ->
        new ClubExpenseVO.History(rs.getLong("id"), rs.getString("action"),
            rs.getString("before_json"), rs.getString("after_json"), rs.getString("reason"),
            rs.getInt("operator_id"), rs.getString("operator_name"), dateTime(rs, "created_at")));
  }

  @Transactional(rollbackFor = Exception.class)
  public ClubExpenseVO create(RequestClubExpenseDTO request) {
    validate(request);
    access.checkClub(request.clubId());
    validateActivity(request.activityId(), request.clubId());
    int actor = access.actor();
    MapSqlParameterSource params = fields(request)
        .addValue("createdBy", actor);
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.update("""
        INSERT INTO club_expense
          (club_id, paid_on, title, category, amount, handled_by, payment_method, activity_id, note, created_by)
        VALUES
          (:clubId, :paidOn, :title, :category, :amount, :handledBy, :paymentMethod, :activityId, :note, :createdBy)
        """, params, key, new String[] {"id"});
    ClubExpenseVO created = find(Objects.requireNonNull(key.getKey()).longValue());
    recordHistory(created.id(), "create", null, created, null, actor);
    return created;
  }

  @Transactional(rollbackFor = Exception.class)
  public ClubExpenseVO update(Long id, RequestClubExpenseDTO request) {
    validate(request);
    ClubExpenseVO before = detail(id);
    requireActive(before);
    if (!before.clubId().equals(request.clubId())) throw new ClubExpenseException(400, "不能变更所属社团");
    requireVersion(request.version(), before.version());
    validateActivity(request.activityId(), request.clubId());
    int actor = access.actor();
    MapSqlParameterSource params = fields(request)
        .addValue("id", id).addValue("version", request.version()).addValue("updatedBy", actor);
    int updated = jdbc.update("""
        UPDATE club_expense SET paid_on = :paidOn, title = :title, category = :category,
          amount = :amount, handled_by = :handledBy, payment_method = :paymentMethod,
          activity_id = :activityId, note = :note, updated_by = :updatedBy,
          version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id = :id AND version = :version AND status = 'active'
        """, params);
    if (updated == 0) throw new ClubExpenseException(409, "记录已被其他人修改，请刷新后重试");
    ClubExpenseVO after = detail(id);
    recordHistory(id, "update", before, after, null, actor);
    return after;
  }

  @Transactional(rollbackFor = Exception.class)
  public ClubExpenseVO voidExpense(Long id, String reason, Integer version) {
    if (reason == null || reason.isBlank() || reason.length() > 500) {
      throw new ClubExpenseException(400, "请填写500字以内的作废原因");
    }
    ClubExpenseVO before = detail(id);
    requireActive(before);
    requireVersion(version, before.version());
    int actor = access.actor();
    int changed = jdbc.update("""
        UPDATE club_expense SET status = 'voided', void_reason = :reason, voided_by = :actor,
          voided_at = CURRENT_TIMESTAMP, updated_by = :actor, updated_at = CURRENT_TIMESTAMP,
          version = version + 1
        WHERE id = :id AND version = :version AND status = 'active'
        """, new MapSqlParameterSource("id", id).addValue("version", version)
        .addValue("reason", reason.trim()).addValue("actor", actor));
    if (changed == 0) throw new ClubExpenseException(409, "记录已被其他人修改，请刷新后重试");
    ClubExpenseVO after = detail(id);
    recordHistory(id, "void", before, after, reason.trim(), actor);
    return after;
  }

  @Transactional(rollbackFor = Exception.class)
  public ClubExpenseVO.Attachment upload(Long expenseId, MultipartFile file) {
    detail(expenseId);
    lockExpense(expenseId);
    ClubExpenseVO before = detail(expenseId);
    requireActive(before);
    if (before.attachmentCount() >= 5) throw new ClubExpenseException(400, "每笔支出最多上传5份凭证");
    ClubExpenseAttachmentStorage.StoredFile stored;
    try {
      stored = storage.store(file);
    } catch (IOException | IllegalArgumentException e) {
      throw new ClubExpenseException(400, e.getMessage());
    }
    try {
      GeneratedKeyHolder key = new GeneratedKeyHolder();
      jdbc.update("""
          INSERT INTO club_expense_attachment
            (expense_id, storage_name, original_name, mime_type, file_size, uploaded_by)
          VALUES (:expenseId, :storageName, :originalName, :mimeType, :size, :actor)
          """, new MapSqlParameterSource("expenseId", expenseId)
          .addValue("storageName", stored.storageName())
          .addValue("originalName", stored.originalName())
          .addValue("mimeType", stored.mimeType()).addValue("size", stored.size())
          .addValue("actor", access.actor()), key, new String[] {"id"});
      ClubExpenseVO after = detail(expenseId);
      recordHistory(expenseId, "attachment_add", before, after, null, access.actor());
      return after.attachments().stream()
          .filter(item -> item.id().equals(Objects.requireNonNull(key.getKey()).longValue()))
          .findFirst().orElseThrow();
    } catch (RuntimeException e) {
      try { storage.delete(stored.storageName()); } catch (IOException ignored) { }
      throw e;
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public void deleteAttachment(Long expenseId, Long attachmentId) {
    detail(expenseId);
    lockExpense(expenseId);
    ClubExpenseVO before = detail(expenseId);
    requireActive(before);
    int changed = jdbc.update("""
        UPDATE club_expense_attachment SET deleted_at = CURRENT_TIMESTAMP, deleted_by = :actor
        WHERE id = :attachmentId AND expense_id = :expenseId AND deleted_at IS NULL
        """, new MapSqlParameterSource("attachmentId", attachmentId)
        .addValue("expenseId", expenseId).addValue("actor", access.actor()));
    if (changed == 0) throw new ClubExpenseException(404, "凭证不存在");
    recordHistory(expenseId, "attachment_remove", before, detail(expenseId), null, access.actor());
  }

  public Download download(Long expenseId, Long attachmentId) {
    detail(expenseId);
    List<Map<String, Object>> rows = jdbc.queryForList("""
        SELECT storage_name, original_name, mime_type, file_size FROM club_expense_attachment
        WHERE id = :attachmentId AND expense_id = :expenseId AND deleted_at IS NULL
        """, new MapSqlParameterSource("attachmentId", attachmentId).addValue("expenseId", expenseId));
    if (rows.isEmpty()) throw new ClubExpenseException(404, "凭证不存在");
    Map<String, Object> row = rows.get(0);
    try {
      return new Download(storage.load((String) row.get("storage_name")),
          (String) row.get("original_name"), (String) row.get("mime_type"),
          ((Number) row.get("file_size")).longValue());
    } catch (IOException e) {
      throw new ClubExpenseException(404, "凭证文件不存在");
    }
  }

  private void lockExpense(Long expenseId) {
    jdbc.queryForList("SELECT id FROM club_expense WHERE id = :id FOR UPDATE",
        new MapSqlParameterSource("id", expenseId));
  }

  private Query query(Filter filter) {
    if (filter == null) filter = new Filter(null, null, null, null, null, "active", null);
    if (filter.startDate() != null && filter.endDate() != null && filter.startDate().isAfter(filter.endDate())) {
      throw new ClubExpenseException(400, "开始日期不能晚于结束日期");
    }
    if (filter.category() != null && !filter.category().isBlank() && !CATEGORIES.contains(filter.category())) {
      throw new ClubExpenseException(400, "支出分类不正确");
    }
    String status = filter.status() == null || filter.status().isBlank() ? "active" : filter.status();
    if (!Set.of("active", "voided", "all").contains(status)) throw new ClubExpenseException(400, "支出状态不正确");
    StringBuilder where = new StringBuilder(" WHERE 1 = 1");
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (filter.clubId() != null) {
      access.checkClub(filter.clubId());
      where.append(" AND e.club_id = :clubId");
      params.addValue("clubId", filter.clubId());
    } else {
      List<Integer> scope = access.allowedClubIds();
      if (scope != null) {
        if (scope.isEmpty()) where.append(" AND 1 = 0");
        else {
          where.append(" AND e.club_id IN (:allowedClubIds)");
          params.addValue("allowedClubIds", scope);
        }
      }
    }
    if (filter.startDate() != null) { where.append(" AND e.paid_on >= :startDate"); params.addValue("startDate", filter.startDate()); }
    if (filter.endDate() != null) { where.append(" AND e.paid_on <= :endDate"); params.addValue("endDate", filter.endDate()); }
    if (filter.category() != null && !filter.category().isBlank()) { where.append(" AND e.category = :category"); params.addValue("category", filter.category()); }
    if (filter.activityId() != null) { where.append(" AND e.activity_id = :activityId"); params.addValue("activityId", filter.activityId()); }
    if (!"all".equals(status)) { where.append(" AND e.status = :status"); params.addValue("status", status); }
    if (filter.keyword() != null && !filter.keyword().isBlank()) {
      where.append(" AND (e.title LIKE :keyword OR e.handled_by LIKE :keyword OR e.note LIKE :keyword)");
      params.addValue("keyword", "%" + filter.keyword().trim() + "%");
    }
    return new Query(where.toString(), params);
  }

  private void validate(RequestClubExpenseDTO request) {
    if (request == null || request.clubId() == null || request.paidOn() == null
        || request.title() == null || request.title().isBlank() || request.title().length() > 160
        || request.amount() == null || request.amount().signum() <= 0
        || request.amount().scale() > 2 || request.amount().precision() - request.amount().scale() > 10
        || request.handledBy() == null || request.handledBy().isBlank() || request.handledBy().length() > 80
        || request.note() != null && request.note().length() > 1000) {
      throw new ClubExpenseException(400, "支出信息不完整或金额格式不正确");
    }
    if (!CATEGORIES.contains(request.category())) throw new ClubExpenseException(400, "支出分类不正确");
    if (request.paymentMethod() != null && !request.paymentMethod().isBlank()
        && !PAYMENT_METHODS.contains(request.paymentMethod())) {
      throw new ClubExpenseException(400, "支付方式不正确");
    }
  }

  private void validateActivity(Integer activityId, Integer clubId) {
    if (activityId == null) return;
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM club_activity WHERE id = :id AND club_id = :clubId",
        new MapSqlParameterSource("id", activityId).addValue("clubId", clubId), Integer.class);
    if (count == null || count == 0) throw new ClubExpenseException(400, "关联活动不属于该社团");
  }

  private void requireActive(ClubExpenseVO expense) {
    if (!"active".equals(expense.status())) throw new ClubExpenseException(409, "已作废记录不能修改");
  }

  private void requireVersion(Integer supplied, Integer current) {
    if (supplied == null || !supplied.equals(current)) {
      throw new ClubExpenseException(409, "记录已被其他人修改，请刷新后重试");
    }
  }

  private ClubExpenseVO find(Long id) {
    if (id == null || id <= 0) throw new ClubExpenseException(404, "支出记录不存在");
    List<ClubExpenseVO> rows = jdbc.query(SELECT + " WHERE e.id = :id",
        new MapSqlParameterSource("id", id), EXPENSE_MAPPER);
    if (rows.isEmpty()) throw new ClubExpenseException(404, "支出记录不存在");
    return rows.get(0);
  }

  private ClubExpenseVO withAttachments(ClubExpenseVO expense) {
    List<ClubExpenseVO.Attachment> attachments = jdbc.query("""
        SELECT id, original_name, mime_type, file_size, created_at
        FROM club_expense_attachment WHERE expense_id = :expenseId AND deleted_at IS NULL
        ORDER BY id
        """, new MapSqlParameterSource("expenseId", expense.id()), (rs, n) ->
        new ClubExpenseVO.Attachment(rs.getLong("id"), rs.getString("original_name"),
            rs.getString("mime_type"), rs.getLong("file_size"), dateTime(rs, "created_at")));
    return new ClubExpenseVO(expense.id(), expense.clubId(), expense.clubName(), expense.paidOn(),
        expense.title(), expense.category(), expense.amount(), expense.handledBy(),
        expense.paymentMethod(), expense.activityId(), expense.activityTitle(), expense.note(),
        expense.status(), expense.voidReason(), expense.createdBy(), expense.updatedBy(),
        expense.voidedBy(), expense.version(), expense.createdAt(), expense.updatedAt(),
        expense.voidedAt(), attachments.size(), attachments);
  }

  private MapSqlParameterSource fields(RequestClubExpenseDTO request) {
    return new MapSqlParameterSource("clubId", request.clubId())
        .addValue("paidOn", request.paidOn()).addValue("title", request.title().trim())
        .addValue("category", request.category()).addValue("amount", request.amount())
        .addValue("handledBy", request.handledBy().trim())
        .addValue("paymentMethod", blankToNull(request.paymentMethod()))
        .addValue("activityId", request.activityId()).addValue("note", blankToNull(request.note()));
  }

  private void recordHistory(Long expenseId, String action, ClubExpenseVO before,
      ClubExpenseVO after, String reason, Integer actor) {
    try {
      jdbc.update("""
          INSERT INTO club_expense_history
            (expense_id, action, before_json, after_json, reason, operator_id)
          VALUES (:expenseId, :action, :beforeJson, :afterJson, :reason, :actor)
          """, new MapSqlParameterSource("expenseId", expenseId).addValue("action", action)
          .addValue("beforeJson", before == null ? null : objectMapper.writeValueAsString(before))
          .addValue("afterJson", after == null ? null : objectMapper.writeValueAsString(after))
          .addValue("reason", reason).addValue("actor", actor));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("无法记录支出变更历史", e);
    }
  }

  private static ClubExpenseVO mapExpense(ResultSet rs, int rowNum) throws SQLException {
    Integer activityId = integerOrNull(rs, "activity_id");
    return new ClubExpenseVO(rs.getLong("id"), rs.getInt("club_id"), rs.getString("club_name"),
        rs.getDate("paid_on").toLocalDate(), rs.getString("title"), rs.getString("category"),
        rs.getBigDecimal("amount"), rs.getString("handled_by"), rs.getString("payment_method"),
        activityId, rs.getString("activity_title"), rs.getString("note"),
        rs.getString("status"), rs.getString("void_reason"), rs.getInt("created_by"),
        integerOrNull(rs, "updated_by"), integerOrNull(rs, "voided_by"), rs.getInt("version"),
        dateTime(rs, "created_at"), dateTime(rs, "updated_at"), dateTime(rs, "voided_at"),
        rs.getInt("attachment_count"), List.of());
  }

  private static Integer integerOrNull(ResultSet rs, String name) throws SQLException {
    int value = rs.getInt(name);
    return rs.wasNull() ? null : value;
  }

  private static LocalDateTime dateTime(ResultSet rs, String name) throws SQLException {
    Timestamp value = rs.getTimestamp(name);
    return value == null ? null : value.toLocalDateTime();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  static String csvCell(String value) {
    if (value == null) return "";
    String safe = value;
    String firstContent = safe.stripLeading();
    if (!firstContent.isEmpty() && "=+-@".indexOf(firstContent.charAt(0)) >= 0) safe = "'" + safe;
    return '"' + safe.replace("\"", "\"\"") + '"';
  }

  private record Query(String where, MapSqlParameterSource params) {}
}
