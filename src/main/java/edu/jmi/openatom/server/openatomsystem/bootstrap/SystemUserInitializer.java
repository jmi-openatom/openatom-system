package edu.jmi.openatom.server.openatomsystem.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jmi.openatom.server.openatomsystem.mapper.PermissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RoleMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.RolePermissionMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserRoleMapper;
import edu.jmi.openatom.server.openatomsystem.entity.Permission;
import edu.jmi.openatom.server.openatomsystem.entity.Role;
import edu.jmi.openatom.server.openatomsystem.entity.RolePermission;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.entity.UserRole;
import edu.jmi.openatom.server.openatomsystem.security.PasswordService;
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
@Order(2)
@RequiredArgsConstructor
public class SystemUserInitializer implements ApplicationRunner {
  private final UserMapper userMapper;
  private final RoleMapper roleMapper;
  private final PermissionMapper permissionMapper;
  private final UserRoleMapper userRoleMapper;
  private final RolePermissionMapper rolePermissionMapper;
  private final PasswordService passwordService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) {
    initRoleTemplate(RoleSeedTemplate.formalMember());
    initRoleTemplate(RoleSeedTemplate.probationaryMember());
    initRoleTemplate(RoleSeedTemplate.departmentHead());
    initRoleTemplate(RoleSeedTemplate.clubAdmin());

    RoleSeedTemplate adminRoleTemplate = RoleSeedTemplate.superAdmin();
    UserSeedTemplate adminTemplate = UserSeedTemplate.admin();
    Role adminRole = initRoleTemplate(adminRoleTemplate);
    User adminUser = ensureUser(adminTemplate);
    bindUserRole(adminUser.getId(), adminRole.getId());
  }

  private Role initRoleTemplate(RoleSeedTemplate template) {
    Role role =
        ensureRole(
            template.code(),
            template.name(),
            template.dataScope(),
            template.description());
    bindRolePermissions(role.getId(), template.permissionCodes());
    return role;
  }

  private Role ensureRole(String code, String name, String dataScope, String description) {
    Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getCode, code));
    if (role != null) {
      return role;
    }

    Role newRole =
        Role.builder()
            .name(name)
            .code(code)
            .dataScope(dataScope)
            .description(description)
            .build();
    roleMapper.insert(newRole);
    log.info("Initialized role: {}", code);
    return newRole;
  }

  /**
   * 用户初始化模板。
   *
   * <p>后续新增种子用户时，照着 {@link UserSeedTemplate} 再加一条模板，然后调用本方法即可。
   */
  private User ensureUser(UserSeedTemplate template) {
    User user =
        userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUserName, template.username())
                .or()
                .eq(User::getStudentId, template.studentId()));
    if (user != null) {
      boolean changed = false;
      if (user.getUserStatus() != template.status()) {
        user.setUserStatus(template.status());
        changed = true;
      }
      if (passwordService.matches(template.rawPassword(), user.getPassword())
          && passwordService.shouldUpgrade(user.getPassword())) {
        user.setPassword(passwordService.encode(template.rawPassword()));
        changed = true;
      }
      if (changed) {
        userMapper.updateById(user);
      }
      return user;
    }

    User newUser =
        User.builder()
            .userName(template.username())
            .studentId(template.studentId())
            .realName(template.realName())
            .phone(template.phone())
            .email(template.email())
            .password(passwordService.encode(template.rawPassword()))
            .userStatus(template.status())
            .build();
    userMapper.insert(newUser);
    log.info("Initialized user from template: username={}", template.username());
    return newUser;
  }

  private void bindUserRole(Integer userId, Integer roleId) {
    UserRole exists =
        userRoleMapper.selectOne(
            new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .eq(UserRole::getRoleId, roleId));
    if (exists == null) {
      userRoleMapper.insert(UserRole.builder().userId(userId).roleId(roleId).build());
    }
  }

  private void bindRolePermissions(Integer roleId, List<String> permissionCodes) {
    List<Integer> permissionIds = permissionCodes.stream().map(this::findPermissionIdByCode).toList();
    for (Integer permissionId : permissionIds) {
      RolePermission exists =
          rolePermissionMapper.selectOne(
              new LambdaQueryWrapper<RolePermission>()
                  .eq(RolePermission::getRoleId, roleId)
                  .eq(RolePermission::getPermissionId, permissionId));
      if (exists == null) {
        rolePermissionMapper.insert(
            RolePermission.builder().roleId(roleId).permissionId(permissionId).build());
      }
    }
  }

  private Integer findPermissionIdByCode(String code) {
    Permission permission =
        permissionMapper.selectOne(new LambdaQueryWrapper<Permission>().eq(Permission::getCode, code));
    if (permission == null) {
      throw new IllegalStateException("Permission not initialized: " + code);
    }
    return permission.getId();
  }
}
