package edu.jmi.openatom.lab.score.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.framework.entity.LabUser;
import edu.jmi.openatom.lab.framework.mapper.LabUserMapper;
import edu.jmi.openatom.lab.framework.mq.LabEventPublisher;
import edu.jmi.openatom.lab.notice.service.NoticeService;
import edu.jmi.openatom.lab.score.entity.LabCheckinScoreLog;
import edu.jmi.openatom.lab.score.mapper.LabCheckinScoreLogMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreService {
  private final LabUserMapper userMapper;
  private final LabCheckinScoreLogMapper checkinScoreLogMapper;
  private final LabEventPublisher eventPublisher;
  private final NoticeService noticeService;

  public void applyLocalScore(Long userId, int delta, Long checkinLogId, String message) {
    if (delta == 0) {
      return;
    }
    LabUser user = userMapper.selectById(userId);
    if (user == null) {
      return;
    }
    int next = Math.max(0, user.getReputationScore() + delta);
    user.setReputationScore(next);
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.updateById(user);
    noticeService.sendToUser(
        userId,
        "SCORE_LOCAL",
        "实验室信誉分已更新",
        message + "，变动 " + delta + " 分，当前剩余 " + next + " 分。");
  }

  public void publishClubScore(LabCheckinScoreLog log, String source) {
    if (log.getClubScoreChange() == null || log.getClubScoreChange() <= 0) {
      return;
    }
    LabUser user = userMapper.selectById(log.getUserId());
    if (user == null) {
      return;
    }
    ClubScoreEvent event =
        new ClubScoreEvent(
            "LAB_CHECKIN_SCORE_ADD",
            log.getId(),
            user.getId(),
            user.getClubUserId(),
            log.getCheckinDate(),
            log.getClubScoreChange(),
            source,
            "lab-checkin-" + log.getId());
    try {
      eventPublisher.publishClubScore(event);
      log.setClubSyncStatus(1);
      log.setSyncError(null);
      checkinScoreLogMapper.updateById(log);
      noticeService.sendToUser(
          user.getId(),
          "SCORE_SYNC",
          "签到加分已进入同步队列",
          "您的今日签到加分已推送至社团管理系统队列（+" + log.getClubScoreChange() + " 积分）。");
    } catch (Exception ex) {
      log.setClubSyncStatus(3);
      log.setSyncError(ex.getMessage());
      checkinScoreLogMapper.updateById(log);
      noticeService.sendToUser(
          user.getId(),
          "SCORE_SYNC_FAILED",
          "签到加分同步失败",
          "RabbitMQ 推送失败，系统已记录待人工处理：" + ex.getMessage());
    }
  }

  public LabDtos.ScoreOverview overview(Long userId) {
    LabUser user = userMapper.selectById(userId);
    Long checkins =
        checkinScoreLogMapper.selectCount(
            new LambdaQueryWrapper<LabCheckinScoreLog>()
                .eq(LabCheckinScoreLog::getUserId, userId)
                .gt(LabCheckinScoreLog::getAttendanceStatus, 0));
    return new LabDtos.ScoreOverview(user == null ? 0 : user.getReputationScore(), 0L, checkins);
  }

  public void markClubSyncSuccess(Long checkinLogId) {
    LabCheckinScoreLog log = checkinScoreLogMapper.selectById(checkinLogId);
    if (log == null) {
      return;
    }
    log.setClubSyncStatus(2);
    log.setSyncError(null);
    checkinScoreLogMapper.updateById(log);
    noticeService.sendToUser(
        log.getUserId(),
        "SCORE_SYNC_DONE",
        "社团积分同步成功",
        "您的今日签到加分已成功同步至社团管理系统（+" + log.getClubScoreChange() + " 积分）。");
  }

  public LabCheckinScoreLog findLog(Long userId, LocalDate date) {
    return checkinScoreLogMapper.selectOne(
        new LambdaQueryWrapper<LabCheckinScoreLog>()
            .eq(LabCheckinScoreLog::getUserId, userId)
            .eq(LabCheckinScoreLog::getCheckinDate, date));
  }
}
