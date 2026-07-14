package edu.jmi.openatom.server.openatomsystem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jmi.openatom.server.openatomsystem.entity.PartnerClub;
import edu.jmi.openatom.server.openatomsystem.mapper.PartnerClubMapper;
import java.util.List;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PartnerClubServiceImplTest {
  @Test
  void returnsPublishedPartnersWithParsedTagsAndSafeWebsiteOnly() {
    PartnerClub safe = partner("https://example.org", "[\"开源\",\"Linux\"]");
    PartnerClub unsafe = partner("javascript:alert(1)", "[]");
    AtomicInteger queryCount = new AtomicInteger();
    PartnerClubMapper mapper = mapperReturning(List.of(safe, unsafe), queryCount);
    PartnerClubServiceImpl partnerClubService = new PartnerClubServiceImpl(mapper, new ObjectMapper());

    var result = partnerClubService.publicList(true, 4);

    assertEquals(0, result.getCode());
    assertEquals(1, result.getData().size());
    assertEquals(List.of("开源", "Linux"), result.getData().getFirst().getTags());
    assertEquals(1, queryCount.get());
  }

  @Test
  void rejectsOutOfRangeLimitBeforeQueryingDatabase() {
    AtomicInteger queryCount = new AtomicInteger();
    PartnerClubMapper mapper = mapperReturning(List.of(), queryCount);
    PartnerClubServiceImpl partnerClubService = new PartnerClubServiceImpl(mapper, new ObjectMapper());

    var result = partnerClubService.publicList(true, 101);

    assertEquals(400, result.getCode());
    assertEquals(0, queryCount.get());
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
    return club;
  }
}
