package edu.jmi.openatom.lab.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.lab.common.dto.LabDtos;
import edu.jmi.openatom.lab.common.web.Result;
import edu.jmi.openatom.lab.framework.auth.LabSecurityContext;
import edu.jmi.openatom.lab.framework.entity.LabUser;
import edu.jmi.openatom.lab.framework.mapper.LabUserMapper;
import edu.jmi.openatom.lab.score.entity.LabCheckinScoreLog;
import edu.jmi.openatom.lab.score.mapper.LabCheckinScoreLogMapper;
import edu.jmi.openatom.lab.score.service.ScoreService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ScoreController {
  private final ScoreService scoreService;
  private final LabCheckinScoreLogMapper scoreLogMapper;
  private final LabUserMapper userMapper;

  @GetMapping("/score/me")
  public Result<LabDtos.ScoreOverview> overview() {
    return Result.success(scoreService.overview(LabSecurityContext.userId()));
  }

  @GetMapping("/score/logs")
  public Result<List<LabDtos.CheckinView>> myLogs() {
    return Result.success(
        scoreLogMapper
            .selectList(
                new LambdaQueryWrapper<LabCheckinScoreLog>()
                    .eq(LabCheckinScoreLog::getUserId, LabSecurityContext.userId())
                    .orderByDesc(LabCheckinScoreLog::getCheckinDate)
                    .last("LIMIT 120"))
            .stream()
            .map(this::toView)
            .toList());
  }

  @GetMapping("/admin/scoreboard")
  public Result<List<LabDtos.ScoreboardRow>> scoreboard() {
    return Result.success(
        userMapper.selectList(new LambdaQueryWrapper<LabUser>().orderByDesc(LabUser::getReputationScore)).stream()
            .map(
                user ->
                    new LabDtos.ScoreboardRow(
                        user.getId(),
                        user.getNickname(),
                        user.getUsername(),
                        user.getLabRole(),
                        user.getReputationScore()))
            .toList());
  }

  @PostMapping("/admin/score-sync/{checkinLogId}/success")
  public Result<String> markClubSyncSuccess(@PathVariable Long checkinLogId) {
    scoreService.markClubSyncSuccess(checkinLogId);
    return Result.success("社团积分同步状态已确认");
  }

  private LabDtos.CheckinView toView(LabCheckinScoreLog log) {
    return new LabDtos.CheckinView(
        log.getId(),
        log.getUserId(),
        log.getCheckinDate(),
        log.getAttendanceStatus(),
        log.getSource(),
        log.getCheckinAt(),
        log.getLocalScoreChange(),
        log.getClubScoreChange(),
        log.getClubSyncStatus(),
        log.getSyncError());
  }
}
