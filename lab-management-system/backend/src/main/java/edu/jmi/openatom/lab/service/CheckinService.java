package edu.jmi.openatom.lab.service;

import edu.jmi.openatom.lab.entity.CheckinScoreLog;
import edu.jmi.openatom.lab.entity.Notification;
import edu.jmi.openatom.lab.repository.CheckinScoreLogRepository;
import edu.jmi.openatom.lab.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinService {
    private final CheckinScoreLogRepository checkinScoreLogRepository;
    private final ScoreService scoreService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void checkinByProblemAC(Long userId, Long problemId) {
        LocalDate today = LocalDate.now();
        CheckinScoreLog log = checkinScoreLogRepository
            .findByUserIdAndCheckinDate(userId, today)
            .orElseGet(() -> {
                CheckinScoreLog newLog = new CheckinScoreLog();
                newLog.setUserId(userId);
                newLog.setCheckinDate(today);
                return newLog;
            });

        if (log.getAttendanceStatus() == 0) {
            log.setAttendanceStatus(1); // 正常(刷题AC)
            log.setClubSyncStatus(1); // 待同步到社团系统
            checkinScoreLogRepository.save(log);
            log.info("用户 {} 通过刷题自动签到", userId);
        }
    }

    @Transactional
    public void manualCheckin(Long userId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        CheckinScoreLog log = checkinScoreLogRepository
            .findByUserIdAndCheckinDate(userId, today)
            .orElseGet(() -> {
                CheckinScoreLog newLog = new CheckinScoreLog();
                newLog.setUserId(userId);
                newLog.setCheckinDate(today);
                return newLog;
            });

        if (log.getAttendanceStatus() == 0) {
            // 09:00 为标准签到时间
            if (now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(9, 30))) {
                log.setAttendanceStatus(3); // 迟到
                log.setLocalScoreChange(-5);
                scoreService.deductReputationScore(userId, 5);
            } else {
                log.setAttendanceStatus(2); // 正常(现场)
                log.setClubSyncStatus(1); // 待同步
            }
            checkinScoreLogRepository.save(log);
        }
    }

    @Scheduled(cron = "0 0 23 * * ?") // 每天 23:00 检查旷课
    @Transactional
    public void checkAbsence() {
        LocalDate today = LocalDate.now();
        // 实际环境需要遍历所有实验室成员
        log.info("检查旷课记录...");
    }
}
