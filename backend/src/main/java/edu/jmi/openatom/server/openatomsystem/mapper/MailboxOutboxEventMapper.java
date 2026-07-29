package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.MailboxOutboxEvent;
import java.sql.Timestamp;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MailboxOutboxEventMapper extends BaseMapper<MailboxOutboxEvent> {

  /**
   * Replays a daily user snapshot so pre-existing users and any cross-system drift are repaired.
   * The date-scoped event id keeps the insert idempotent across multiple application instances.
   */
  @Insert(
      """
      INSERT IGNORE INTO mailbox_outbox_event
        (event_id, event_type, aggregate_id, payload_json)
      SELECT CONCAT('reconcile-', DATE_FORMAT(UTC_DATE(), '%Y%m%d'), '-', u.id),
             'USER_RECONCILE',
             CAST(u.id AS CHAR),
             JSON_OBJECT(
               'sub', CAST(u.id AS CHAR),
               'userId', u.id,
               'username', u.user_name,
               'displayName', u.real_name,
               'status', CASE u.user_status
                           WHEN 1 THEN 'ACTIVE'
                           WHEN 2 THEN 'LOCKED'
                           ELSE 'DISABLED'
                         END)
      FROM tb_user u
      """)
  int enqueueDailyReconciliation();

  @Update(
      """
      UPDATE mailbox_outbox_event
      SET status = 'PROCESSING', processing_started_at = CURRENT_TIMESTAMP
      WHERE id = #{id}
        AND status IN ('PENDING', 'RETRY')
        AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)
      """)
  int claim(@Param("id") Long id);

  @Update(
      """
      UPDATE mailbox_outbox_event
      SET status = 'PENDING', next_retry_at = NULL, processing_started_at = NULL,
          last_error = 'stale_processing_recovered'
      WHERE status = 'PROCESSING'
        AND processing_started_at < #{staleBefore}
      """)
  int recoverStale(@Param("staleBefore") Timestamp staleBefore);

  @Update(
      """
      UPDATE mailbox_outbox_event
      SET status = 'PROCESSED', processed_at = CURRENT_TIMESTAMP,
          next_retry_at = NULL, processing_started_at = NULL, last_error = NULL
      WHERE id = #{id} AND status = 'PROCESSING'
      """)
  int markProcessed(@Param("id") Long id);

  @Update(
      """
      UPDATE mailbox_outbox_event
      SET status = 'RETRY', retry_count = #{retryCount},
          next_retry_at = #{nextRetryAt}, processing_started_at = NULL, last_error = #{reason}
      WHERE id = #{id} AND status = 'PROCESSING'
      """)
  int markRetry(
      @Param("id") Long id,
      @Param("retryCount") int retryCount,
      @Param("nextRetryAt") Timestamp nextRetryAt,
      @Param("reason") String reason);

  @Update(
      """
      UPDATE mailbox_outbox_event
      SET status = 'FAILED', retry_count = #{retryCount},
          next_retry_at = NULL, processing_started_at = NULL, last_error = #{reason}
      WHERE id = #{id} AND status = 'PROCESSING'
      """)
  int markFailed(
      @Param("id") Long id,
      @Param("retryCount") int retryCount,
      @Param("reason") String reason);
}
