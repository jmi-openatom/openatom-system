package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.dto.RequestSavePartnerClubDTO;
import edu.jmi.openatom.server.openatomsystem.entity.PartnerClub;
import edu.jmi.openatom.server.openatomsystem.entity.User;
import edu.jmi.openatom.server.openatomsystem.mapper.PartnerClubMapper;
import edu.jmi.openatom.server.openatomsystem.mapper.UserMapper;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PartnerClubServiceImplTest {
  @Test
  void returnsPublishedPartnersWithParsedTagsAndOptionalWebsite() {
    PartnerClub safe = partner("https://example.org", "[\"开源\",\"Linux\"]");
    PartnerClub withoutWebsite = partner(null, "[]");
    PartnerClub unsafe = partner("javascript:alert(1)", "[]");
    AtomicInteger queryCount = new AtomicInteger();
    PartnerClubMapper mapper = mapperReturning(List.of(safe, withoutWebsite, unsafe), queryCount);
    User president = president();
    PartnerClubServiceImpl partnerClubService =
        new PartnerClubServiceImpl(mapper, userMapperReturning(president), new ObjectMapper());

    var result = partnerClubService.publicList(true, 4);

    assertEquals(0, result.getCode());
    assertEquals(2, result.getData().size());
    assertEquals(List.of("开源", "Linux"), result.getData().getFirst().getTags());
    assertEquals("张同学", result.getData().getFirst().getPresidentName());
    assertEquals(
        "/api/v1/files/images/president.png",
        result.getData().getFirst().getPresidentAvatarUrl());
    assertNull(result.getData().get(1).getWebsiteUrl());
    assertEquals(1, queryCount.get());
  }

  @Test
  void rejectsOutOfRangeLimitBeforeQueryingDatabase() {
    AtomicInteger queryCount = new AtomicInteger();
    PartnerClubMapper mapper = mapperReturning(List.of(), queryCount);
    PartnerClubServiceImpl partnerClubService =
        new PartnerClubServiceImpl(mapper, userMapperReturning(null), new ObjectMapper());

    var result = partnerClubService.publicList(true, 101);

    assertEquals(400, result.getCode());
    assertEquals(0, queryCount.get());
  }

  @Test
  void rejectsUnknownPresidentUser() {
    PartnerClubMapper mapper = mapperReturning(List.of(), new AtomicInteger());
    PartnerClubServiceImpl partnerClubService =
        new PartnerClubServiceImpl(mapper, userMapperReturning(null), new ObjectMapper());
    RequestSavePartnerClubDTO request = new RequestSavePartnerClubDTO();
    request.setName("示例开源伙伴");
    request.setLogoUrl("/api/v1/files/images/example.png");
    request.setDescription("共同推动校园开源文化建设。");
    request.setPresidentUserId(99);

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> partnerClubService.create(request));

    assertEquals("绑定的社长用户不存在", error.getMessage());
  }

  private UserMapper userMapperReturning(User user) {
    return (UserMapper)
        Proxy.newProxyInstance(
            UserMapper.class.getClassLoader(),
            new Class<?>[] {UserMapper.class},
            (proxy, method, args) -> {
              if (method.getName().equals("selectBatchIds")) {
                Collection<?> ids = (Collection<?>) args[0];
                return user != null && ids.contains(user.getId()) ? List.of(user) : List.of();
              }
              if (method.getName().equals("selectById")) {
                return user != null && user.getId().equals(args[0]) ? user : null;
              }
              if (method.getName().equals("toString")) return "UserMapperTestDouble";
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private PartnerClubMapper mapperReturning(
      List<PartnerClub> rows, AtomicInteger queryCount) {
    return (PartnerClubMapper)
        Proxy.newProxyInstance(
            PartnerClubMapper.class.getClassLoader(),
            new Class<?>[] {PartnerClubMapper.class},
            (proxy, method, args) -> {
              if (method.getName().equals("selectPublished")) {
                queryCount.incrementAndGet();
                return rows;
              }
              if (method.getName().equals("toString")) return "PartnerClubMapperTestDouble";
              throw new UnsupportedOperationException(method.getName());
            });
  }

  private PartnerClub partner(String websiteUrl, String tags) {
    PartnerClub club = new PartnerClub();
    club.setId(1);
    club.setName("示例开源伙伴");
    club.setLogoUrl("/api/v1/files/images/example.png");
    club.setDescription("共同推动校园开源文化建设。");
    club.setWebsiteUrl(websiteUrl);
    club.setTags(tags);
    club.setSortOrder(10);
    club.setFeatured(true);
    club.setPresidentUserId(7);
    return club;
  }

  private User president() {
    return User.builder()
        .id(7)
        .userName("zhang")
        .realName("张同学")
        .avatar("/api/v1/files/images/president.png")
        .build();
  }
}
