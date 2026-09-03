package edu.jmi.openatom.server.openatomsystem.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import edu.jmi.openatom.server.openatomsystem.common.Result;
import edu.jmi.openatom.server.openatomsystem.entity.Interview;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewInterviewer;
import edu.jmi.openatom.server.openatomsystem.entity.InterviewRecording;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewInterviewerMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.InterviewRecordingMapper;
import edu.jmi.openatom.server.openatomsystem.service.InterviewRecordingService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseInterviewRecordingVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InterviewRecordingServiceImpl implements InterviewRecordingService {
  private static final long MAX_FILE_SIZE = 500L * 1024 * 1024;

  private final InterviewMapper interviewMapper;
  private final InterviewInterviewerMapper interviewerMapper;
  private final InterviewRecordingMapper recordingMapper;

  @Value("${app.interview-recording.storage-dir:./uploads/interview-recordings}")
  private String storageDir;

  @Override
  public Result<List<ResponseInterviewRecordingVO>> list(Integer interviewId) {
    if (!canAccess(interviewId, StpUtil.getLoginIdAsInt())) return Result.error(404, "面试录音不存在");
    return Result.success(recordingMapper.selectByInterviewId(interviewId).stream()
        .map(ResponseInterviewRecordingVO::from).toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewRecordingVO> upload(
      Integer interviewId, MultipartFile file, Integer durationSeconds) {
    int userId = StpUtil.getLoginIdAsInt();
    if (!canAccess(interviewId, userId)) return Result.error(403, "您不是本场面试官");
    try {
      StoredAudio stored = store(file);
      InterviewRecording recording = InterviewRecording.builder().interviewId(interviewId)
          .interviewerId(userId).fileName(stored.fileName()).mimeType(stored.mediaType().toString())
          .fileSize(file.getSize()).durationSeconds(validDuration(durationSeconds)).build();
      recordingMapper.insert(recording);
      return Result.success(ResponseInterviewRecordingVO.from(recording), "录音已保存");
    } catch (IllegalArgumentException exception) {
      return Result.error(422, exception.getMessage());
    } catch (IOException exception) {
      return Result.error(500, "录音保存失败，请重试");
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Result<ResponseInterviewRecordingVO> updateTranscript(
      Integer interviewId, Long recordingId, String transcript) {
    int userId = StpUtil.getLoginIdAsInt();
    if (!canAccess(interviewId, userId)) return Result.error(403, "您不是本场面试官");
    InterviewRecording recording = recordingId == null ? null : recordingMapper.selectById(recordingId);
    if (recording == null || !interviewId.equals(recording.getInterviewId())) {
      return Result.error(404, "面试录音不存在");
    }
    if (!userIdEquals(recording.getInterviewerId(), userId)) return Result.error(403, "只能编辑本人录音的转写");
    recording.setTranscript(transcript == null ? "" : transcript.trim());
    recording.setTranscriptUpdatedAt(new Timestamp(System.currentTimeMillis()));
    recordingMapper.updateById(recording);
    return Result.success(ResponseInterviewRecordingVO.from(recording), "转写已保存");
  }

  @Override
  public AudioResource loadAudio(Long recordingId) throws IOException {
    InterviewRecording recording = recordingId == null ? null : recordingMapper.selectById(recordingId);
    if (recording == null || !canAccess(recording.getInterviewId(), StpUtil.getLoginIdAsInt())) return null;
    Path target = root().resolve(recording.getFileName()).normalize();
    if (!target.getParent().equals(root()) || !Files.exists(target) || !Files.isRegularFile(target)) return null;
    Resource resource = new UrlResource(target.toUri());
    if (!resource.exists() || !resource.isReadable()) return null;
    return new AudioResource(resource, MediaType.parseMediaType(recording.getMimeType()), Files.size(target));
  }

  private StoredAudio store(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择有效的录音文件");
    if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("单段录音不能超过 500MB");
    MediaType mediaType = safeMediaType(file.getContentType());
    String fileName = UUID.randomUUID() + extensionOf(mediaType);
    Path root = root();
    Files.createDirectories(root);
    Path target = root.resolve(fileName).normalize();
    if (!target.getParent().equals(root)) throw new IOException("非法录音文件名");
    try (InputStream input = file.getInputStream()) {
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
    }
    return new StoredAudio(fileName, mediaType);
  }

  private boolean canAccess(Integer interviewId, int userId) {
    Interview interview = interviewId == null ? null : interviewMapper.selectById(interviewId);
    if (interview == null) return false;
    return interviewerMapper.selectByInterviewId(interviewId).stream()
        .map(InterviewInterviewer::getInterviewerId).anyMatch(id -> userIdEquals(id, userId));
  }

  private Path root() { return Paths.get(storageDir).toAbsolutePath().normalize(); }

  private MediaType safeMediaType(String raw) {
    MediaType value;
    try { value = raw == null ? null : MediaType.parseMediaType(raw); }
    catch (IllegalArgumentException ignored) { value = null; }
    if (value == null || !"audio".equalsIgnoreCase(value.getType())) {
      throw new IllegalArgumentException("仅支持音频录音文件");
    }
    return value;
  }

  private String extensionOf(MediaType type) {
    String subtype = type.getSubtype().toLowerCase(Locale.ROOT);
    if (subtype.contains("webm")) return ".webm";
    if (subtype.contains("ogg")) return ".ogg";
    if (subtype.contains("mp4")) return ".m4a";
    if (subtype.contains("mpeg")) return ".mp3";
    return ".audio";
  }

  private Integer validDuration(Integer seconds) {
    return seconds == null || seconds < 0 ? null : Math.min(seconds, 24 * 60 * 60);
  }

  private boolean userIdEquals(Integer left, int right) { return left != null && left == right; }

  private record StoredAudio(String fileName, MediaType mediaType) {}
}
