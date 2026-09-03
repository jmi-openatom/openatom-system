package edu.jmi.openatom.server.openatomsystem.vo;

import edu.jmi.openatom.server.openatomsystem.entity.InterviewRecording;
import java.sql.Timestamp;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResponseInterviewRecordingVO {
  Long id;
  Integer interviewerId;
  String mimeType;
  Long fileSize;
  Integer durationSeconds;
  String transcript;
  Timestamp transcriptUpdatedAt;
  Timestamp createdAt;

  public static ResponseInterviewRecordingVO from(InterviewRecording recording) {
    return builder().id(recording.getId()).interviewerId(recording.getInterviewerId())
        .mimeType(recording.getMimeType()).fileSize(recording.getFileSize())
        .durationSeconds(recording.getDurationSeconds()).transcript(recording.getTranscript())
        .transcriptUpdatedAt(recording.getTranscriptUpdatedAt()).createdAt(recording.getCreatedAt()).build();
  }
}
