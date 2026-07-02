package edu.jmi.openatom.server.openatomsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 文件导出任务服务（Redis 版本）
 * 
 * <p>管理异步导出任务，支持任务状态查询和结果下载
 * 使用 Redis 存储任务状态，支持多实例部署
 */
@Slf4j
@Service
public class ExportTaskService {

  @Autowired
  private FileMigrationServiceImpl fileMigrationService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  @Qualifier("exportTaskExecutor")
  private Executor taskExecutor;

  // 使用 Spring 自动配置的 ObjectMapper（已注册 JavaTimeModule 等所有必要模块）
  @Autowired
  private ObjectMapper springObjectMapper;

  // 用于序列化的 ObjectMapper（备用，确保有 JavaTimeModule）
  private ObjectMapper objectMapper() {
    return springObjectMapper;
  }

  // Redis Key 前缀
  private static final String TASK_KEY_PREFIX = "export:task:";
  private static final String TASK_INDEX_KEY = "export:tasks:index";
  
  // 临时文件目录
  private static final String TEMP_EXPORT_DIR = "./uploads/temp-exports";
  
  // 备份目录
  private static final String BACKUP_DIR = "./uploads/export-backups";
  
  // 任务过期时间（秒）：24小时
  private static final int TASK_EXPIRE_SECONDS = 86400;

  /**
   * 启动异步导出任务
   * 
   * @return 任务信息
   */
  public ExportTask startExport() {
    String taskId = UUID.randomUUID().toString();
    ExportTask task = new ExportTask(taskId);
    
    try {
      // 保存到 Redis
      saveTaskToRedis(task);
      
      // 添加到任务索引（用于清理）
      redisTemplate.opsForSet().add(TASK_INDEX_KEY, taskId);
      
      // 设置过期时间
      redisTemplate.expire(TASK_KEY_PREFIX + taskId, TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("保存任务到 Redis 失败: {}", e.getMessage(), e);
      throw new RuntimeException("启动导出任务失败: " + e.getMessage(), e);
    }

    // 通过线程池异步执行导出（避免 @Async 自调用不生效问题）
    String finalTaskId = taskId;
    taskExecutor.execute(() -> executeExportAsync(finalTaskId));

    log.info("启动导出任务: {}", taskId);
    return task;
  }

  /**
   * 获取任务状态
   * 
   * @param taskId 任务ID
   * @return 任务信息
   */
  public ExportTask getTaskStatus(String taskId) {
    ExportTask task = loadTaskFromRedis(taskId);
    if (task == null) {
      throw new IllegalArgumentException("任务不存在或已过期: " + taskId);
    }
    return task;
  }

  /**
   * 更新任务状态
   * 
   * @param task 任务对象
   */
  private void updateTaskInRedis(ExportTask task) {
    try {
      saveTaskToRedis(task);
      // 刷新过期时间
      redisTemplate.expire(TASK_KEY_PREFIX + task.getTaskId(), TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("更新任务状态失败: {}", task.getTaskId(), e);
    }
  }

  /**
   * 获取导出文件路径
   * 
   * @param taskId 任务ID
   * @return 文件路径
   */
  public Path getExportFilePath(String taskId) throws IOException {
    ExportTask task = loadTaskFromRedis(taskId);
    if (task == null) {
      throw new IllegalArgumentException("任务不存在或已过期: " + taskId);
    }
    if (!"completed".equals(task.getStatus())) {
      throw new IllegalStateException("任务未完成，无法下载: " + task.getStatus());
    }
    if (task.getFilePath() == null) {
      throw new IllegalStateException("文件路径为空");
    }
    return Path.of(task.getFilePath());
  }

  /**
   * 清理过期任务（从 Redis 索引中查找并删除）
   */
  public void cleanupExpiredTasks() {
    log.info("开始清理过期导出任务...");
    
    try {
      // 从 Redis 获取所有任务 ID
      java.util.Set<String> taskIds = redisTemplate.opsForSet().members(TASK_INDEX_KEY);
      if (taskIds == null || taskIds.isEmpty()) {
        log.info("没有需要清理的任务");
        return;
      }
      
      int cleanedCount = 0;
      LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
      
      for (String taskId : taskIds) {
        ExportTask task = loadTaskFromRedis(taskId);
        
        // 如果任务不存在（已过期被 Redis 自动删除），从索引中移除
        if (task == null) {
          redisTemplate.opsForSet().remove(TASK_INDEX_KEY, taskId);
          cleanedCount++;
          continue;
        }
        
        // 检查是否超过24小时
        if (task.getCreatedAt().isBefore(cutoff)) {
          // 删除临时文件
          if (task.getFilePath() != null) {
            try {
              Files.deleteIfExists(Path.of(task.getFilePath()));
            } catch (IOException e) {
              log.warn("删除过期导出文件失败: {}", task.getFilePath(), e);
            }
          }
          
          // 从 Redis 删除任务和索引
          redisTemplate.delete(TASK_KEY_PREFIX + taskId);
          redisTemplate.opsForSet().remove(TASK_INDEX_KEY, taskId);
          
          cleanedCount++;
          log.info("清理过期任务: {}", taskId);
        }
      }
      
      log.info("过期导出任务清理完成，共清理 {} 个任务", cleanedCount);
    } catch (Exception e) {
      log.error("清理过期导出任务失败", e);
    }
  }

  /**
   * 创建导出文件备份
   * 
   * @param taskId 任务ID
   * @param sourceFile 源文件路径
   * @return 备份文件路径
   */
  public Path createBackup(String taskId, Path sourceFile) throws IOException {
    // 确保备份目录存在
    Path backupDir = Path.of(BACKUP_DIR);
    if (!Files.exists(backupDir)) {
      Files.createDirectories(backupDir);
    }
    
    // 生成备份文件名（带时间戳）
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String backupFileName = "backup-" + taskId.substring(0, 8) + "-" + timestamp + ".zip";
    Path backupFile = backupDir.resolve(backupFileName);
    
    // 复制文件
    Files.copy(sourceFile, backupFile);
    
    log.info("创建导出备份: {} -> {}", sourceFile, backupFile);
    return backupFile;
  }

  /**
   * 自动化备份：每天凌晨3点备份当天完成的导出文件
   */
  public void autoBackupCompletedTasks() {
    log.info("开始自动化备份导出文件...");
    
    try {
      java.util.Set<String> taskIds = redisTemplate.opsForSet().members(TASK_INDEX_KEY);
      if (taskIds == null || taskIds.isEmpty()) {
        log.info("没有需要备份的任务");
        return;
      }
      
      int backupCount = 0;
      LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
      
      for (String taskId : taskIds) {
        ExportTask task = loadTaskFromRedis(taskId);
        
        if (task == null || !"completed".equals(task.getStatus())) {
          continue;
        }
        
        // 只备份今天完成的任务
        if (task.getCreatedAt().isAfter(todayStart) && task.getFilePath() != null) {
          try {
            Path sourceFile = Path.of(task.getFilePath());
            if (Files.exists(sourceFile)) {
              createBackup(taskId, sourceFile);
              backupCount++;
            }
          } catch (IOException e) {
            log.error("备份任务 {} 失败", taskId, e);
          }
        }
      }
      
      log.info("自动化备份完成，共备份 {} 个文件", backupCount);
    } catch (Exception e) {
      log.error("自动化备份失败", e);
    }
  }

  /**
   * 异步执行导出任务（通过线程池调用，不需要 @Async 注解）
   */
  void executeExportAsync(String taskId) {
    log.info("开始执行异步导出任务: {}", taskId);
    ExportTask task = loadTaskFromRedis(taskId);
    if (task == null) {
      log.error("异步导出: 任务不存在于 Redis: {}", taskId);
      return;
    }

    try {
      task.setStatus("processing");
      task.setProgress(10);
      task.setMessage("正在准备导出...");
      updateTaskInRedis(task);

      // 确保临时目录存在
      Path tempDir = Path.of(TEMP_EXPORT_DIR);
      if (!Files.exists(tempDir)) {
        Files.createDirectories(tempDir);
      }

      // 生成临时文件名
      String fileName = "openatom-files-" + 
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + 
          ".zip";
      Path tempFile = tempDir.resolve(fileName);

      task.setProgress(30);
      task.setMessage("正在打包文件...");
      updateTaskInRedis(task);

      // 执行导出到临时文件
      long startTime = System.currentTimeMillis();
      try (FileOutputStream fos = new FileOutputStream(tempFile.toFile());
           ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        
        FileMigrationServiceImpl.ExportResult result = fileMigrationService.exportAll(baos);
        byte[] zipData = baos.toByteArray();
        fos.write(zipData);
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("导出任务 {} 完成: {} 个文件, {} MB, 耗时 {} ms", 
            taskId, result.getTotalFiles(), zipData.length / (1024 * 1024), elapsed);
      }

      task.setProgress(100);
      task.setStatus("completed");
      task.setFileName(fileName);
      task.setFileSize(Files.size(tempFile));
      task.setFilePath(tempFile.toString());
      task.setMessage("导出完成");
      updateTaskInRedis(task);

      // 创建备份
      try {
        createBackup(taskId, tempFile);
      } catch (IOException e) {
        log.warn("创建备份失败，但不影响主流程", e);
      }

    } catch (Exception e) {
      log.error("导出任务 {} 失败", taskId, e);
      task.setStatus("failed");
      task.setError(e.getMessage());
      task.setMessage("导出失败: " + e.getMessage());
      updateTaskInRedis(task);
      
      // 清理可能的临时文件
      if (task.getFilePath() != null) {
        try {
          Files.deleteIfExists(Path.of(task.getFilePath()));
        } catch (IOException ignored) {
        }
      }
    }
  }

  // ========== Redis 辅助方法 ==========

  private void saveTaskToRedis(ExportTask task) throws Exception {
    String key = TASK_KEY_PREFIX + task.getTaskId();
    String json = objectMapper().writeValueAsString(task);
    redisTemplate.opsForValue().set(key, json);
    log.debug("保存任务到 Redis: key={}, json={}", key, json);
  }

  private ExportTask loadTaskFromRedis(String taskId) {
    try {
      String key = TASK_KEY_PREFIX + taskId;
      String json = redisTemplate.opsForValue().get(key);
      if (json == null) {
        log.warn("Redis 中找不到任务: {}", taskId);
        return null;
      }
      ExportTask task = objectMapper().readValue(json, ExportTask.class);
      log.debug("从 Redis 加载任务成功: {}, status={}", taskId, task.getStatus());
      return task;
    } catch (Exception e) {
      log.error("从 Redis 加载任务失败: {}, 错误: {}", taskId, e.getMessage(), e);
      return null;
    }
  }

  @Data
  @NoArgsConstructor
  public static class ExportTask {
    private String taskId;
    private String status; // pending, processing, completed, failed
    private Integer progress; // 0-100
    private String message;
    private String fileName;
    private Long fileSize;
    private String filePath;
    private String error;
    private LocalDateTime createdAt;

    public ExportTask(String taskId) {
      this.taskId = taskId;
      this.status = "pending";
      this.progress = 0;
      this.createdAt = LocalDateTime.now();
    }
  }
}
