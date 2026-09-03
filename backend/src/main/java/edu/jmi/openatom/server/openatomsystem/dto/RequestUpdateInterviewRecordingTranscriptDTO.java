package edu.jmi.openatom.server.openatomsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestUpdateInterviewRecordingTranscriptDTO {
  @Size(max = 200000, message = "转写文本不能超过 200000 个字符")
  private String transcript;
}
