package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import cn.dev33.satoken.SaManager;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {
  private static final String TOKEN = "ABCDEFGH";
  private static final String TOKEN_KEY = "openatom:group-join:token:" + TOKEN;

  @BeforeEach
  void setUpToken() {
    SaManager.getSaTokenDao().set(TOKEN_KEY, "7", 1800);
  }

  @AfterEach
  void clearToken() {
    SaManager.getSaTokenDao().delete(TOKEN_KEY);
  }

  @Test
  void groupJoinConfirmationAlsoBindsRequestingQq() {
    User user = User.builder().id(7).userName("member").build();
    AtomicReference<User> updatedUser = new AtomicReference<>();
    AuthServiceImpl authService = authService(userMapper(user, null, updatedUser));

    var result = authService.confirmGroupJoin(TOKEN, "3208629021");

    assertEquals(0, result.getCode());
    assertEquals("已确认加入QQ群并完成QQ绑定", result.getData());
    assertEquals("3208629021", user.getQqOpenid());
    assertNotNull(user.getQqGroupJoinedAt());
    assertNull(SaManager.getSaTokenDao().get(TOKEN_KEY));
    assertEquals(user, updatedUser.get());
  }

  @Test
  void groupJoinDoesNotTakeOverQqBoundToAnotherAccount() {
    User user = User.builder().id(7).userName("member").build();
    User otherUser = User.builder().id(8).userName("other").qqOpenid("3208629021").build();
    AtomicReference<User> updatedUser = new AtomicReference<>();
    AuthServiceImpl authService = authService(userMapper(user, otherUser, updatedUser));

    var result = authService.confirmGroupJoin(TOKEN, "3208629021");

    assertEquals(409, result.getCode());
    assertNull(user.getQqOpenid());
    assertNull(user.getQqGroupJoinedAt());
    assertNull(updatedUser.get());
    assertEquals("7", SaManager.getSaTokenDao().get(TOKEN_KEY));
  }

  private AuthServiceImpl authService(UserMapper userMapper) {
    return new AuthServiceImpl(
        userMapper,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private UserMapper userMapper(
      User user, User boundUser, AtomicReference<User> updatedUser) {
    return (UserMapper)
        Proxy.newProxyInstance(
            UserMapper.class.getClassLoader(),
            new Class<?>[] {UserMapper.class},
            (proxy, method, args) -> {
              if (method.getName().equals("selectById")) {
                return user.getId().equals(args[0]) ? user : null;
              }
              if (method.getName().equals("selectByQqOpenid")) return boundUser;
              if (method.getName().equals("updateById")) {
                updatedUser.set((User) args[0]);
                return 1;
              }
              if (method.getName().equals("toString")) return "UserMapperTestDouble";
              throw new UnsupportedOperationException(method.getName());
            });
  }
}
