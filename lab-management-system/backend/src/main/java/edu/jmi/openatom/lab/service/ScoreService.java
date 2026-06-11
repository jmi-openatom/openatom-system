package edu.jmi.openatom.lab.service;

import edu.jmi.openatom.lab.entity.CheckinScoreLog;
import edu.jmi.openatom.lab.entity.LabUser;
import edu.jmi.openatom.lab.entity.Notification;
import edu.jmi.openatom.lab.repository.CheckinScoreLogRepository;
import edu.jmi.openatom.lab.repository.LabUserRepository;
import edu.jmi.openatom.lab.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {
    private final LabUserRepository labUserRepository;
    private final CheckinScoreLogRepository checkinScoreLogRepository;
    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${lab.cms.score-api-url}")
    private String cmsScoreApiUrl;

    @Transactional
    public void deductReputationScore(Long userId, int points) {
        LabUser user = labUserRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setReputationScore(user.getReputationScore() - points);
        labUserRepository.save(user);

        // 发送扣分通知
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("实验室信誉分扣除");
        notification.setContent(String.format("因您今日未按时出勤，系统已扣除实验室信誉分 %d 分，当前剩余 %d 分。",
            points, user.getReputationScore()));
        notification.setType("ATTENDANCE_WARNING");
        notificationRepository.save(notification);
    }

    @Scheduled(cron = "0 */5 * * * ?") // 每 5 分钟同步一次
    @Transactional
    public void syncClubScores() {
        List<CheckinScoreLog> pendingLogs = checkinScoreLogRepository.findByClubSyncStatus(1);

        for (CheckinScoreLog log : pendingLogs) {
            try {
                syncToClub(log);
                log.setClubSyncStatus(2); // 同步成功

                // 发送同步成功通知
                Notification notification = new Notification();
                notification.setUserId(log.getUserId());
                notification.setTitle("社团积分同步成功");
                notification.setContent("您的今日签到加分已成功同步至社团管理系统（+10 积分）。");
                notification.setType("SCORE_SYNC");
                notificationRepository.save(notification);
            } catch (Exception e) {
                log.setClubSyncStatus(3); // 同步失败
                log.error("同步社团积分失败: userId={}, error={}", log.getUserId(), e.getMessage());
            }
            checkinScoreLogRepository.save(log);
        }
    }

    private void syncToClub(CheckinScoreLog log) {
        LabUser user = labUserRepository.findById(log.getUserId())
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        Map<String, Object> request = new HashMap<>();
        request.put("userId", user.getClubUserId());
        request.put("points", 10);
        request.put("reason", "实验室每日签到");

        // 模拟调用 CMS API（实际环境替换为真实调用）
        // restTemplate.postForObject(cmsScoreApiUrl, request, Map.class);
        log.info("同步社团积分: clubUserId={}, points=10", user.getClubUserId());
    }
}
