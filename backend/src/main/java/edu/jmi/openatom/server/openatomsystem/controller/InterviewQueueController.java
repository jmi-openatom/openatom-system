package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestMoveInterviewRoomDTO;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewQueueOperation;
import edu.jmi.openatom.server.openatomsystem.service.InterviewQueueService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewQueueVO;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InterviewQueueController {
  private final InterviewQueueService service;

  @GetMapping("/interview-sessions/{sessionId}/queue")
  @SaCheckPermission("interview:list")
  public Result<ResponseInterviewQueueVO> detail(@PathVariable Integer sessionId) {
    return service.detail(sessionId);
  }

  @GetMapping("/site/interview-call-screens/{sessionId}")
  public Result<ResponseInterviewQueueVO> callScreen(@PathVariable Integer sessionId) {
    return service.callScreen(sessionId);
  }

  @PostMapping("/interviews/{interviewId}/check-in")
  @SaCheckPermission("interview:update")
  public Result<String> checkIn(@PathVariable Integer interviewId) { return service.checkIn(interviewId); }

  @PostMapping("/interviews/{interviewId}/undo-check-in")
  @SaCheckPermission("interview:update")
  public Result<String> undoCheckIn(@PathVariable Integer interviewId) { return service.undoCheckIn(interviewId); }

  @PostMapping("/interview-rooms/{roomId}/call-next")
  @SaCheckPermission("interview:update")
  public Result<ResponseInterviewQueueVO.Candidate> callNext(@PathVariable Integer roomId) {
    return service.callNext(roomId);
  }

  @PostMapping("/interview-rooms/{roomId}/call-again")
  @SaCheckPermission("interview:update")
  public Result<ResponseInterviewQueueVO.Candidate> callAgain(@PathVariable Integer roomId) {
    return service.callAgain(roomId);
  }

  @PostMapping("/interviews/{interviewId}/no-show")
  @SaCheckPermission("interview:update")
  public Result<String> markNoShow(@PathVariable Integer interviewId) {
    return service.markNoShow(interviewId);
  }

  @PostMapping("/interviews/{interviewId}/restore-waiting")
  @SaCheckPermission("interview:update")
  public Result<String> restoreWaiting(@PathVariable Integer interviewId) {
    return service.restoreWaiting(interviewId);
  }

  @PostMapping("/interviews/{interviewId}/move-room")
  @SaCheckPermission("interview:update")
  public Result<String> moveRoom(@PathVariable Integer interviewId,
      @Valid @RequestBody RequestMoveInterviewRoomDTO request) {
    return service.moveRoom(interviewId, request.getTargetRoomId());
  }

  @PostMapping("/interview-rooms/{roomId}/recover")
  @SaCheckPermission("interview:update")
  public Result<String> recoverRoom(@PathVariable Integer roomId) { return service.recoverRoom(roomId); }

  @PostMapping("/interview-sessions/{sessionId}/complete")
  @SaCheckPermission("interview:update")
  public Result<String> completeSession(@PathVariable Integer sessionId) {
    return service.completeSession(sessionId);
  }

  @PostMapping("/interview-sessions/{sessionId}/reopen")
  @SaCheckPermission("interview:update")
  public Result<String> reopenSession(@PathVariable Integer sessionId) {
    return service.reopenSession(sessionId);
  }

  @GetMapping("/interview-sessions/{sessionId}/queue-operations")
  @SaCheckPermission("interview:update")
  public Result<List<InterviewQueueOperation>> operations(@PathVariable Integer sessionId) {
    return service.operations(sessionId);
  }

  @GetMapping("/interview-sessions/{sessionId}/evaluation-summary.csv")
  @SaCheckPermission("interview:update")
  public ResponseEntity<byte[]> exportEvaluationSummary(@PathVariable Integer sessionId) {
    Result<byte[]> result = service.exportEvaluationSummary(sessionId);
    if (result.getCode() != Result.SUCCESS_CODE) return ResponseEntity.notFound().build();
    String name = URLEncoder.encode("面试评价汇总-" + sessionId + ".csv", StandardCharsets.UTF_8)
        .replace("+", "%20");
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(result.getData());
  }
}
