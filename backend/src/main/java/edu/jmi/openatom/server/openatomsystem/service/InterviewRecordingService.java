package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewRecordingVO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface InterviewRecordingService {
  Result<List<ResponseInterviewRecordingVO>> list(Integer interviewId);

  Result<ResponseInterviewRecordingVO> upload(Integer interviewId, MultipartFile file, Integer durationSeconds);

  Result<ResponseInterviewRecordingVO> updateTranscript(
      Integer interviewId, Long recordingId, String transcript);

  AudioResource loadAudio(Long recordingId) throws IOException;

  record AudioResource(org.springframework.core.io.Resource resource,
                       org.springframework.http.MediaType mediaType,
                       long contentLength) {}
}
