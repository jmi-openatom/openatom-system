package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.mapper.MembershipApplicationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时自动补绑孤立的匿名招新申请
 *
 * <p>扫描所有 user_id 为 NULL 的申请, 按学号匹配到已有用户并绑定。
 * 解决在自动绑定代码部署之前已导入用户但未绑定的历史数据问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnonymousApplicationBindingInitializer implements ApplicationRunner {
  private final MembershipApplicationMapper membershipApplicationMapper;

  @Override
  public void run(ApplicationArguments args) {
    try {
      int bound = membershipApplicationMapper.bindAllOrphanApplications();
      if (bound > 0) {
        log.info("启动时补绑 {} 条孤立匿名申请", bound);
      }
    } catch (Exception e) {
      log.warn("启动时补绑匿名申请失败: {}", e.getMessage());
    }
  }
}
