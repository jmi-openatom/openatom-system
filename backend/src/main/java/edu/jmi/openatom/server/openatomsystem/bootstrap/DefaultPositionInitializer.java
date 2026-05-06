package edu.jmi.openatom.server.openatomsystem.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.entity.ClubDepartment;
import edu.jmi.openatom.server.openatomsystem.entity.ClubPosition;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubDepartmentMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.ClubPositionMapper;
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
@Order(5)
@RequiredArgsConstructor
public class DefaultPositionInitializer implements ApplicationRunner {
  private static final List<DefaultPositionSeed> DEFAULT_POSITIONS =
      List.of(
          new DefaultPositionSeed("部门负责人", 1),
          new DefaultPositionSeed("成员", null));

  private final ClubDepartmentMapper clubDepartmentMapper;
  private final ClubPositionMapper clubPositionMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    List<ClubDepartment> departments =
        clubDepartmentMapper.selectList(
            new LambdaQueryWrapper<ClubDepartment>().orderByAsc(ClubDepartment::getId));
    for (ClubDepartment department : departments) {
      for (DefaultPositionSeed seed : DEFAULT_POSITIONS) {
        ensurePosition(department, seed);
      }
    }
  }

  private void ensurePosition(ClubDepartment department, DefaultPositionSeed seed) {
    ClubPosition exists =
        clubPositionMapper.selectOne(
            new LambdaQueryWrapper<ClubPosition>()
                .eq(ClubPosition::getClubId, department.getClubId())
                .eq(ClubPosition::getDepartmentId, department.getId())
                .eq(ClubPosition::getName, seed.name()));
    if (exists != null) {
      if (!java.util.Objects.equals(exists.getMaxCount(), seed.maxCount())) {
        exists.setMaxCount(seed.maxCount());
        clubPositionMapper.updateById(exists);
      }
      return;
    }

    clubPositionMapper.insert(
        ClubPosition.builder()
            .clubId(department.getClubId())
            .departmentId(department.getId())
            .name(seed.name())
            .maxCount(seed.maxCount())
            .build());
    log.info(
        "Initialized default position: clubId={}, departmentId={}, name={}",
        department.getClubId(),
        department.getId(),
        seed.name());
  }

  private record DefaultPositionSeed(String name, Integer maxCount) {}
}
