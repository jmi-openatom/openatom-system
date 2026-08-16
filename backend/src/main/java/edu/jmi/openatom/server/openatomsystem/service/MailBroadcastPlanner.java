package edu.jmi.openatom.server.openatomsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.common.EmailMarkdown;
import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import edu.jmi.openatom.server.openatomsystem.config.MailOutboxProperties;
import edu.jmi.openatom.server.openatomsystem.entity.MailboxOutboxEvent;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.MailboxOutboxEventMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.service.impl.DeepSeekClientService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Enqueues branded broadcast mails (notifications, activity publishes,
 * recruitment opens, approval/interview results) into the mail outbox after
 * the triggering action commits. Content is generated with DeepSeek, falling
 * back to a plain styled text when AI is unavailable.
 */
@Slf4j
@Service
public class MailBroadcastPlanner {
  private static final String EVENT_TYPE = "mail_broadcast";

  private final MailboxOutboxEventMapper outboxMapper;
  private final DeepSeekClientService deepSeek;
  private final MailOutboxProperties properties;
  private final UserMapper userMapper;
  private final MailboxProvisioningClient mailboxClient;

  public MailBroadcastPlanner(
      MailboxOutboxEventMapper outboxMapper,
      DeepSeekClientService deepSeek,
      MailOutboxProperties properties,
      UserMapper userMapper,
      MailboxProvisioningClient mailboxClient) {
    this.outboxMapper = outboxMapper;
    this.deepSeek = deepSeek;
    this.properties = properties;
    this.userMapper = userMapper;
    this.mailboxClient = mailboxClient;
  }

  /** Broadcast to every mail recipient (active mailboxes + main-site external emails). */
  public void enqueueAllBroadcast(
      String eventId, String kind, String subject, String userPayload) {
    if (!properties.enabled() || eventId == null || eventId.isBlank()) {
      return;
    }
    Thread.startVirtualThread(
        () -> {
          try {
            String html = generatedHtml(kind, userPayload);
            insertEvent(eventId, kind, subject, html, null);
          } catch (Exception exception) {
            log.error("mail broadcast planning failed for eventId={}", eventId, exception);
          }
        });
  }

  /** Sends to a single user's mailbox: external email first, then the mail-system address. */
  public void enqueueUserMail(
      String eventId, Integer userId, String kind, String subject, String userPayload) {
    if (userId == null) {
      return;
    }
    enqueueUserBroadcast(eventId, List.of(userId), kind, subject, userPayload);
  }

  /** Sends to each user's mailbox (external email or mail-system address). */
  public void enqueueUserBroadcast(
      String eventId, List<Integer> userIds, String kind, String subject, String userPayload) {
    if (!properties.enabled()
        || eventId == null
        || eventId.isBlank()
        || userIds == null
        || userIds.isEmpty()) {
      return;
    }
    Thread.startVirtualThread(
        () -> {
          try {
            List<String> emails =
                userIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .flatMap(userId -> resolveUserEmails(userId).stream())
                    .distinct()
                    .toList();
            if (emails.isEmpty()) {
              log.warn("mail broadcast: no email addresses for eventId={}, skipping", eventId);
              return;
            }
            String html = generatedHtml(kind, userPayload);
            insertEvent(eventId, kind, subject, html, emails);
          } catch (Exception exception) {
            log.error("mail broadcast planning failed for eventId={}", eventId, exception);
          }
        });
  }

  private List<String> resolveUserEmails(Integer userId) {
    List<String> emails = new ArrayList<>();
    User user = userMapper.selectById(userId);
    if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
      emails.add(user.getEmail().trim());
    }
    MailboxProvisioningClient.MailboxStatus status = mailboxClient.queryStatus(userId);
    if (status != null && status.address() != null && !status.address().isBlank()) {
      emails.add(status.address());
    }
    return emails.stream().distinct().toList();
  }

  private void insertEvent(
      String eventId, String kind, String subject, String html, List<String> recipients) {
    Long existing =
        outboxMapper.selectCount(
            new LambdaQueryWrapper<MailboxOutboxEvent>()
                .eq(MailboxOutboxEvent::getEventId, eventId));
    if (existing != null && existing > 0) {
      return;
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("kind", kind);
    payload.put("subject", subject);
    payload.put("htmlBody", html);
    payload.put("textBody", "");
    payload.put("recipients", recipients);
    MailboxOutboxEvent event =
        MailboxOutboxEvent.builder()
            .eventId(eventId)
            .eventType(EVENT_TYPE)
            .aggregateId(eventId)
            .payloadJson(Jsons.stringify(payload))
            .status("PENDING")
            .retryCount(0)
            .createdAt(new Timestamp(System.currentTimeMillis()))
            .build();
    outboxMapper.insert(event);
    log.info("mail broadcast queued: eventId={} kind={} recipients={}", eventId, kind,
        recipients == null ? "ALL" : recipients.size());
  }

  private String generatedHtml(String kind, String userPayload) {
    try {
      String output =
          deepSeek.chat(
              "mail_broadcast",
              systemPrompt(kind),
              List.of(Map.of("role", "user", "content", userPayload == null ? "" : userPayload)));
      if (output == null || output.isBlank() || output.length() > 100_000) {
        return fallbackHtml(userPayload);
      }
      return looksLikeHtml(output) ? output : EmailMarkdown.render(output);
    } catch (Exception exception) {
      log.warn("mail broadcast AI generation failed, using fallback: {}", exception.getMessage());
      return fallbackHtml(userPayload);
    }
  }

  /** HTML tag presence check; AI occasionally emits markdown/plain text despite the prompt. */
  private boolean looksLikeHtml(String value) {
    String lower = value.toLowerCase();
    return lower.contains("<p") || lower.contains("<h")
        || lower.contains("<ul") || lower.contains("<ol")
        || lower.contains("<div") || lower.contains("<strong")
        || lower.contains("<br");
  }

  private String systemPrompt(String kind) {
    if ("activity".equals(kind)) {
      return """
          你是开放原子开源社团的邮件小编。请根据提供的活动信息，生成一封吸引人的活动邀请邮件正文。
          请使用 Markdown 或纯文本输出（可用 # 标题、- 列表、**加粗**），不要输出 HTML 标签。
          结构建议：问候语（各位同学：）→ 活动亮点 → 时间、地点、报名方式（用列表）→ 邀请语。
          结尾署名：—— 开放原子开源社团。
          """;
    }
    if ("recruitment".equals(kind)) {
      return """
          你是开放原子开源社团的邮件小编。请根据提供的招新信息，生成一封鼓舞人心的招新宣传邮件正文。
          请使用 Markdown 或纯文本输出（可用 # 标题、- 列表、**加粗**），不要输出 HTML 标签。
          结构建议：问候语（各位同学：）→ 社团亮点 → 报名时间与方式（用列表）→ 热情邀请。
          结尾署名：—— 开放原子开源社团。
          """;
    }
    return """
        你是开放原子开源社团的邮件小编。请根据提供的通知内容，生成一封简洁友好的邮件正文。
        请使用 Markdown 或纯文本输出（可用 # 标题、- 列表、**加粗**），不要输出 HTML 标签。
        开头问候语用"同学，你好："，结尾署名：—— 开放原子开源社团。
        """;
  }

  private String fallbackHtml(String userPayload) {
    return EmailMarkdown.render(
        "同学，你好：\n\n" + (userPayload == null ? "" : userPayload) + "\n\n—— 开放原子开源社团");
  }
}
