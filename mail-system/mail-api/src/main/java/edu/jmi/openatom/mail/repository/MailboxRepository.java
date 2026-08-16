package edu.jmi.openatom.mail.repository;

import edu.jmi.openatom.mail.domain.MailboxAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MailboxRepository {
  private final JdbcTemplate jdbc;
  private final RowMapper<MailboxAccount> rowMapper = this::mapAccount;

  public MailboxRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void ensureShell(String sub, long userId, String displayName, String domain, long quota) {
    jdbc.update(
        """
        INSERT IGNORE INTO mailbox_account
          (oauth_sub, user_id, display_name, mail_domain, quota_bytes, status, provision_status)
        VALUES (?, ?, ?, ?, ?, 'PENDING', 'PENDING')
        """,
        sub, userId, displayName, domain, quota);
  }

  public MailboxAccount lockBySub(String sub) {
    return jdbc.queryForObject(
        "SELECT * FROM mailbox_account WHERE oauth_sub = ? FOR UPDATE", rowMapper, sub);
  }

  public Optional<MailboxAccount> findBySub(String sub) {
    List<MailboxAccount> matches =
        jdbc.query("SELECT * FROM mailbox_account WHERE oauth_sub = ?", rowMapper, sub);
    return matches.stream().findFirst();
  }

  public Optional<MailboxAccount> findByEvent(String eventId) {
    List<MailboxAccount> matches =
        jdbc.query(
            """
            SELECT a.* FROM mailbox_processed_event e
            JOIN mailbox_account a ON a.id = e.mailbox_id
            WHERE e.event_id = ?
            """,
            rowMapper,
            eventId);
    return matches.stream().findFirst();
  }

  public List<MailboxAccount> findAll() {
    return jdbc.query("SELECT * FROM mailbox_account ORDER BY id DESC", rowMapper);
  }

  public boolean assignAddress(long id, String displayName, String localPart, String address) {
    try {
      jdbc.update(
          "INSERT INTO mailbox_alias (mailbox_id, alias_address, is_primary) VALUES (?, ?, TRUE)",
          id,
          address);
      jdbc.update(
          """
          UPDATE mailbox_account
          SET display_name = ?, local_part = ?, primary_address = ?,
              status = 'PROVISIONING', provision_status = 'PROVISIONING', last_error = NULL
          WHERE id = ?
          """,
          displayName, localPart, address, id);
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }

  public void markWaiting(long id, String displayName, String eventId) {
    jdbc.update(
        """
        UPDATE mailbox_account SET display_name = ?, status = 'WAITING_PROFILE',
          provision_status = 'WAITING_PROFILE', last_event_id = ?, last_error = NULL
        WHERE id = ?
        """,
        displayName, eventId, id);
  }

  public void markActive(long id, String stalwartId, String eventId) {
    jdbc.update(
        """
        UPDATE mailbox_account SET stalwart_account_id = ?, status = 'ACTIVE',
          provision_status = 'ACTIVE', last_event_id = ?, last_error = NULL
        WHERE id = ?
        """,
        stalwartId, eventId, id);
  }

  public void markSuspended(long id, String eventId) {
    jdbc.update(
        """
        UPDATE mailbox_account SET status = 'SUSPENDED', provision_status = 'ACTIVE',
          last_event_id = ?, last_error = NULL WHERE id = ?
        """,
        eventId, id);
  }

  public List<String> aliases(long mailboxId) {
    return jdbc.queryForList(
        "SELECT alias_address FROM mailbox_alias WHERE mailbox_id = ? ORDER BY id",
        String.class,
        mailboxId);
  }

  public void setPrimaryAlias(long id, String localPart, String address) {
    jdbc.update(
        "INSERT INTO mailbox_alias (mailbox_id, alias_address, is_primary) VALUES (?, ?, TRUE)",
        id,
        address);
    jdbc.update(
        "UPDATE mailbox_alias SET is_primary = (alias_address = ?) WHERE mailbox_id = ?",
        address,
        id);
    jdbc.update(
        "UPDATE mailbox_account SET local_part = ?, primary_address = ? WHERE id = ?",
        localPart, address, id);
  }

  public boolean trySetPrimaryAlias(long id, String localPart, String address) {
    try {
      setPrimaryAlias(id, localPart, address);
      return true;
    } catch (DuplicateKeyException exception) {
      return false;
    }
  }

  public void recordProcessed(String eventId, long mailboxId) {
    jdbc.update(
        "INSERT INTO mailbox_processed_event (event_id, mailbox_id) VALUES (?, ?)",
        eventId, mailboxId);
  }

  private MailboxAccount mapAccount(ResultSet rs, int rowNum) throws SQLException {
    return new MailboxAccount(
        rs.getLong("id"),
        rs.getString("oauth_sub"),
        rs.getLong("user_id"),
        rs.getString("display_name"),
        rs.getString("primary_address"),
        rs.getString("local_part"),
        rs.getString("mail_domain"),
        rs.getString("stalwart_account_id"),
        rs.getLong("quota_bytes"),
        rs.getString("status"),
        rs.getString("provision_status"),
        rs.getString("last_event_id"),
        rs.getString("last_error"));
  }
}