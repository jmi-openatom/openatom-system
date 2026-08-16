package edu.jmi.openatom.mail.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BroadcastLogRepository {
  private final JdbcTemplate jdbc;
  private final RowMapper<BroadcastLogEntry> rowMapper = this::mapEntry;

  public BroadcastLogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void record(
      String source,
      String kind,
      String subject,
      String sender,
      int recipients,
      int batches,
      String messageIds,
      String status,
      String error) {
    jdbc.update(
        """
        INSERT INTO mail_broadcast_log
          (source, kind, subject, sender, recipients, batches, message_ids, status, error)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        source,
        kind,
        truncate(subject, 255),
        truncate(sender, 255),
        recipients,
        batches,
        messageIds,
        status,
        truncate(error, 500));
  }

  public BroadcastLogPage page(int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safeSize = Math.min(100, Math.max(1, pageSize));
    Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM mail_broadcast_log", Integer.class);
    List<BroadcastLogEntry> rows =
        jdbc.query(
            """
            SELECT * FROM mail_broadcast_log
            ORDER BY id DESC
            LIMIT ? OFFSET ?
            """,
            rowMapper,
            safeSize,
            (safePage - 1) * safeSize);
    return new BroadcastLogPage(rows, total == null ? 0 : total, safePage, safeSize);
  }

  private BroadcastLogEntry mapEntry(ResultSet rs, int rowNum) throws SQLException {
    return new BroadcastLogEntry(
        rs.getLong("id"),
        rs.getString("source"),
        rs.getString("kind"),
        rs.getString("subject"),
        rs.getString("sender"),
        rs.getInt("recipients"),
        rs.getInt("batches"),
        rs.getString("message_ids"),
        rs.getString("status"),
        rs.getString("error"),
        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toString());
  }

  private String truncate(String value, int max) {
    if (value == null) return null;
    return value.length() <= max ? value : value.substring(0, max);
  }

  public record BroadcastLogEntry(
      long id,
      String source,
      String kind,
      String subject,
      String sender,
      int recipients,
      int batches,
      String messageIds,
      String status,
      String error,
      String createdAt) {}

  public record BroadcastLogPage(
      List<BroadcastLogEntry> rows, int total, int page, int pageSize) {}
}
