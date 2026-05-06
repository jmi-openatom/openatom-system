package edu.jmi.openatom.server.openatomsystem.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.bootstrap.PermissionSeedCatalog.PermissionSeed;
import edu.jmi.openatom.server.openatomsystem.mapper.PermissionMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SystemPermissionInitializer implements ApplicationRunner {
  private final PermissionMapper permissionMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    for (PermissionSeed seed : PermissionSeedCatalog.all()) {
      initPermission(seed);
    }
  }

  private Permission initPermission(PermissionSeed seed) {
    Permission permission =
        permissionMapper.selectOne(
            new LambdaQueryWrapper<Permission>().eq(Permission::getCode, seed.code()));
    if (permission != null) {
      boolean changed = false;
      if (!Objects.equals(permission.getName(), seed.name())) {
        permission.setName(seed.name());
        changed = true;
      }
      if (!Objects.equals(permission.getType(), seed.type())) {
        permission.setType(seed.type());
        changed = true;
      }
      if (!Objects.equals(permission.getPath(), seed.path())) {
        permission.setPath(seed.path());
        changed = true;
      }
      if (!Objects.equals(permission.getMethod(), seed.method())) {
        permission.setMethod(seed.method());
        changed = true;
      }
      if (changed) {
        permissionMapper.updateById(permission);
      }
      return permission;
    }

    Permission newPermission =
        Permission.builder()
            .name(seed.name())
            .code(seed.code())
            .type(seed.type())
            .path(seed.path())
            .method(seed.method())
            .build();
    permissionMapper.insert(newPermission);
    log.info("Initialized permission: {}", seed.code());
    return newPermission;
  }
}
