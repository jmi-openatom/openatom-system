package edu.jmi.openatom.server.openatomsystem.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Club;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class DefaultDepartmentInitializer implements ApplicationRunner {
  private static final List<DefaultDepartmentSeed> DEFAULT_DEPARTMENTS =
      List.of(
          new DefaultDepartmentSeed("项目部", "负责项目研发、技术实践与项目交付"),
          new DefaultDepartmentSeed("宣传组", "负责宣传物料、内容运营与对外传播"),
          new DefaultDepartmentSeed("活动部", "负责活动策划、组织执行与现场协调"),
          new DefaultDepartmentSeed("外联部", "负责校内外联络、资源合作与关系维护"));

  private final ClubMapper clubMapper;
  private final ClubDepartmentMapper clubDepartmentMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    List<Club> clubs = clubMapper.selectList(null);
    for (Club club : clubs) {
      for (DefaultDepartmentSeed seed : DEFAULT_DEPARTMENTS) {
        ensureDepartment(club.getId(), seed);
      }
    }
  }

  private void ensureDepartment(Integer clubId, DefaultDepartmentSeed seed) {
    ClubDepartment exists =
        clubDepartmentMapper.selectOne(
            new LambdaQueryWrapper<ClubDepartment>()
                .eq(ClubDepartment::getClubId, clubId)
                .eq(ClubDepartment::getName, seed.name()));
    if (exists != null) {
      return;
    }

    clubDepartmentMapper.insert(
        ClubDepartment.builder()
            .clubId(clubId)
            .name(seed.name())
            .description(seed.description())
            .build());
    log.info("Initialized default department: clubId={}, name={}", clubId, seed.name());
  }

  private record DefaultDepartmentSeed(String name, String description) {}
}
