package edu.jmi.openatom.server.openatomsystem.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.dto.RequestUpdateInterviewRecordingTranscriptDTO;
import edu.jmi.openatom.server.openatomsystem.service.InterviewRecordingService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewRecordingVO;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class InterviewRecordingController {
  private final InterviewRecordingService recordingService;

  @GetMapping("/interviews/{interviewId}/recordings")
  @SaCheckPermission("interview:feedback")
  public Result<List<ResponseInterviewRecordingVO>> list(@PathVariable Integer interviewId) {
    return recordingService.list(interviewId);
  }

  @PostMapping("/interviews/{interviewId}/recordings")
  @SaCheckPermission("interview:feedback")
  public Result<ResponseInterviewRecordingVO> upload(
      @PathVariable Integer interviewId,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) Integer durationSeconds) {
    return recordingService.upload(interviewId, file, durationSeconds);
  }

  @PatchMapping("/interviews/{interviewId}/recordings/{recordingId}/transcript")
  @SaCheckPermission("interview:feedback")
  public Result<ResponseInterviewRecordingVO> updateTranscript(
      @PathVariable Integer interviewId, @PathVariable Long recordingId,
      @Valid @RequestBody RequestUpdateInterviewRecordingTranscriptDTO request) {
    return recordingService.updateTranscript(interviewId, recordingId, request.getTranscript());
  }

  @GetMapping("/interview-recordings/{recordingId}/audio")
  @SaCheckPermission("interview:feedback")
  public ResponseEntity<?> audio(@PathVariable Long recordingId) throws IOException {
    InterviewRecordingService.AudioResource audio = recordingService.loadAudio(recordingId);
    if (audio == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(audio.mediaType())
        .contentLength(audio.contentLength()).body(audio.resource());
  }
}
