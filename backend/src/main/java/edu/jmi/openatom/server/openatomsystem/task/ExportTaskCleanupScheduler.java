package edu.jmi.openatom.server.openatomsystem.task;

import edu.jmi.openatom.server.openatomsystem.service.impl.ExportTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 导出任务清理与备份调度器
 * 
 * <p>定时清理过期任务和自动化备份导出文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskCleanupScheduler {

  private final ExportTaskService exportTaskService;

  /**
   * 每天凌晨2点执行清理（删除超过24小时的任务和临时文件）
   */
  @Scheduled(cron = "0 0 2 * * ?")
  public void cleanupExpiredTasks() {
    log.info("=== 开始清理过期导出任务 ===");
    try {
      exportTaskService.cleanupExpiredTasks();
      log.info("=== 过期导出任务清理完成 ===");
    } catch (Exception e) {
      log.error("清理过期导出任务失败", e);
    }
  }

  /**
   * 每天凌晨3点执行自动化备份（备份当天完成的导出文件）
   */
  @Scheduled(cron = "0 0 3 * * ?")
  public void autoBackup() {
    log.info("=== 开始自动化备份导出文件 ===");
    try {
      exportTaskService.autoBackupCompletedTasks();
      log.info("=== 自动化备份完成 ===");
    } catch (Exception e) {
      log.error("自动化备份失败", e);
    }
  }
}
