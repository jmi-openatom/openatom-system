package edu.jmi.openatom.server.openatomsystem.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class DefaultClubInitializer implements ApplicationRunner {
  private static final String DEFAULT_CLUB_NAME = "开放原子开源社团";
  private static final String DEFAULT_CLUB_CODE = "JMI-OPENATOM";

  private final ClubMapper clubMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    Club club =
        clubMapper.selectOne(new LambdaQueryWrapper<Club>().eq(Club::getCode, DEFAULT_CLUB_CODE));
    if (club != null) {
      if (!DEFAULT_CLUB_NAME.equals(club.getName())) {
        club.setName(DEFAULT_CLUB_NAME);
        clubMapper.updateById(club);
      }
      return;
    }

    clubMapper.insert(
        Club.builder()
            .name(DEFAULT_CLUB_NAME)
            .code(DEFAULT_CLUB_CODE)
            .category("open_source")
            .description("开放原子开源社团")
            .status("active")
            .recruitmentStatus("open")
            .build());
    log.info("Initialized default club: {}", DEFAULT_CLUB_CODE);
  }
}
