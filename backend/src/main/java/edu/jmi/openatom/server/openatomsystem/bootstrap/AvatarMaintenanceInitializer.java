package edu.jmi.openatom.server.openatomsystem.bootstrap;

import edu.jmi.openatom.server.openatomsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarMaintenanceInitializer implements ApplicationRunner {
  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) {
    Integer cleaned = userService.cleanupInvalidAvatars().getData();
    if (cleaned != null && cleaned > 0) {
      log.info("Cleaned {} invalid managed avatar records on startup", cleaned);
    }
  }
}
