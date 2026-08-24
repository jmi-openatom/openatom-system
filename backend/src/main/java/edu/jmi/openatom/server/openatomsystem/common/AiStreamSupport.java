package edu.jmi.openatom.server.openatomsystem.common;

import edu.jmi.openatom.server.openatomsystem.common.Jsons;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 流式输出（SSE）统一基建。
 *
 * <p>统一 SseEmitter 的创建、事件封装、心跳与错误处理：
 * <ul>
 *   <li>心跳：15 秒发送一次注释帧，防止代理/Nginx 因空闲断开长流；</li>
 *   <li>超时：默认 5 分钟（覆盖 DeepSeek 最长 180 秒超时 + 心跳余量）；</li>
 *   <li>断开感知：客户端断开后 send 抛 IOException，心跳线程自动退出；</li>
 *   <li>事件协议：phase / delta / session / message / complete / error。</li>
 * </ul>
 */
@Slf4j
@Component
public class AiStreamSupport {
  private static final long EMITTER_TIMEOUT_MS = 300_000L;
  private static final long HEARTBEAT_INTERVAL_MS = 15_000L;

  /** 创建带心跳的 SSE emitter；调用方在流结束时调用 {@link #finish}。 */
  public SseEmitter newEmitter() {
    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
    Thread heartbeat =
        Thread.startVirtualThread(
            () -> {
              while (true) {
                try {
                  Thread.sleep(HEARTBEAT_INTERVAL_MS);
                  emitter.send(SseEmitter.event().comment(": keep-alive"));
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  return;
                } catch (Exception e) {
                  // 客户端已断开，结束心跳
                  return;
                }
              }
            });
    emitter.onCompletion(heartbeat::interrupt);
    emitter.onTimeout(heartbeat::interrupt);
    emitter.onError(error -> heartbeat.interrupt());
    return emitter;
  }

  /** 发送命名事件（data 为 JSON 字符串）。客户端断开时抛出 IOException 由调用方结束流程。 */
  public void emit(SseEmitter emitter, String eventName, Map<String, Object> data)
      throws IOException {
    emitter.send(SseEmitter.event().name(eventName).data(Jsons.stringify(data)));
  }

  /** 发送阶段提示（如"AI 正在澄清活动需求"）。 */
  public void phase(SseEmitter emitter, String message) throws IOException {
    emit(emitter, "phase", Map.of("message", message));
  }

  /** 发送内容增量。 */
  public void delta(SseEmitter emitter, String content) throws IOException {
    emit(emitter, "delta", Map.of("content", content));
  }

  /** 发送完整会话快照。 */
  public void snapshot(SseEmitter emitter, String event, Map<String, Object> detail)
      throws IOException {
    emit(emitter, event, Map.of("detail", detail));
  }

  /** 发送错误事件并结束流（错误信息不向客户端暴露堆栈）。 */
  public void fail(SseEmitter emitter, String message) {
    try {
      emit(
          emitter,
          "error",
          Map.of(
              "message",
              message == null || message.isBlank() ? "AI 流式输出失败" : message));
    } catch (Exception ignored) {
      // 客户端可能已断开
    } finally {
      finish(emitter);
    }
  }

  /** 正常结束流（幂等）。 */
  public void finish(SseEmitter emitter) {
    try {
      emitter.complete();
    } catch (Exception ignored) {
      // 已超时或已结束
    }
  }

  /** 在 finally 中安全结束流，即使 send 已抛 IOException。 */
  public void completeQuietly(SseEmitter emitter) {
    finish(emitter);
  }
}