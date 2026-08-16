package edu.jmi.openatom.mail.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.jmi.openatom.mail.domain.ProvisionRequest;
import edu.jmi.openatom.mail.domain.ProvisionResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:mail;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.session.store-type=none",
      "mail.domain=jmi-openatom.cn",
      "mail.address-salt=test-address-salt-with-32-characters",
      "mail.internal-service-token=test-internal-token-with-32-characters",
      "mail.stalwart.api-url=http://127.0.0.1:1/api",
      "mail.stalwart.session-url=http://127.0.0.1:1/.well-known/jmap",
      "mail.stalwart.jmap-url=http://127.0.0.1:1/jmap",
      "mail.stalwart.api-token=test-stalwart-token-with-32-characters",
      "mail.stalwart.domain-id=test-domain-id-123",
      "mail.oauth.issuer=http://oauth.test/issuer",
      "mail.oauth.authorization-url=http://oauth.test/authorize",
      "mail.oauth.token-url=http://oauth.test/token",
      "mail.oauth.jwks-url=http://oauth.test/jwks",
      "mail.oauth.client-id=mail-test",
      "mail.oauth.client-secret=test-oauth-secret-with-32-characters",
      "mail.oauth.redirect-uri=http://mail.test/api/oauth/callback"
    })
@Transactional
@DirtiesContext
@Import(MailboxProvisioningServiceIntegrationTest.FakeStalwartConfiguration.class)
class MailboxProvisioningServiceIntegrationTest {
  @Autowired private MailboxProvisioningService service;
  @Autowired private RecordingStalwartClient stalwart;

  @BeforeEach
  void resetFakeClient() {
    stalwart.ensuredSubjects.clear();
    stalwart.disabledAccounts.clear();
    stalwart.lastAliases = List.of();
  }

  @Test
  void provisionsWaitForActivationAndActivateWithPinyinAssignsAddress() {
    ProvisionResponse first = service.provision(request("evt-1", "42", 42L, "张三", "ACTIVE"));
    ProvisionResponse duplicate = service.provision(request("evt-1", "42", 42L, "张三", "ACTIVE"));

    assertThat(first.address()).isNull();
    assertThat(first.provisionStatus()).isEqualTo("WAITING_PROFILE");
    assertThat(duplicate).isEqualTo(first);
    assertThat(stalwart.ensuredSubjects).isEmpty();

    ProvisionResponse activated = service.activateWithPinyin("42", "张三");
    assertThat(activated.address()).isEqualTo("zhangsan@jmi-openatom.cn");
    assertThat(stalwart.ensuredSubjects).containsExactly("42");
    assertThat(stalwart.lastAliases).containsExactly("zhangsan@jmi-openatom.cn");
  }

  @Test
  void sameNameGetsDistinctStablePrivacySuffix() {
    service.provision(request("evt-a", "100", 100L, "张三", "ACTIVE"));
    service.provision(request("evt-b", "101", 101L, "张三", "ACTIVE"));

    ProvisionResponse first = service.activateWithPinyin("100", "张三");
    ProvisionResponse second = service.activateWithPinyin("101", "张三");
    assertThat(first.address()).isEqualTo("zhangsan@jmi-openatom.cn");
    assertThat(second.address()).matches("zhangsan\\.[a-z2-7]{4}@jmi-openatom\\.cn");
    assertThat(second.address()).doesNotContain("101");
  }

  @Test
  void missingNameWaitsAndLaterActivationUsesUpdatedName() {
    ProvisionResponse waiting = service.provision(request("evt-w1", "200", 200L, null, "ACTIVE"));
    service.provision(request("evt-w2", "200", 200L, "李雷", "ACTIVE"));

    assertThat(waiting.provisionStatus()).isEqualTo("WAITING_PROFILE");
    assertThat(waiting.address()).isNull();
    ProvisionResponse active = service.activateWithPinyin("200", "李雷");
    assertThat(active.address()).isEqualTo("lilei@jmi-openatom.cn");
  }

  @Test
  void disabledUserIsSuspendedAndRecoveryKeepsAddress() {
    service.provision(request("evt-s1", "300", 300L, "王五", "ACTIVE"));
    ProvisionResponse active = service.activateWithPinyin("300", "王五");
    ProvisionResponse disabled = service.provision(request("evt-s2", "300", 300L, "王五", "DISABLED"));
    ProvisionResponse restored = service.provision(request("evt-s3", "300", 300L, "王五", "ACTIVE"));

    assertThat(disabled.status()).isEqualTo("SUSPENDED");
    assertThat(restored.address()).isEqualTo(active.address());
    assertThat(stalwart.disabledAccounts).contains("stalwart-1");
  }

  @Test
  void pronunciationCorrectionMakesNewPrimaryAndPreservesOldAlias() {
    service.provision(request("evt-c1", "400", 400L, "曾乐", "ACTIVE"));
    service.activateWithPinyin("400", "曾乐");
    ProvisionResponse corrected = service.correctPrimaryAddress("400", "zengyue");

    assertThat(corrected.address()).isEqualTo("zengyue@jmi-openatom.cn");
    assertThat(stalwart.lastAliases)
        .containsExactly("cengle@jmi-openatom.cn", "zengyue@jmi-openatom.cn");
  }

  private ProvisionRequest request(
      String eventId, String sub, long userId, String displayName, String status) {
    return new ProvisionRequest(
        eventId, "USER_UPDATED", sub, userId, "user" + userId, displayName, status);
  }

  @TestConfiguration
  static class FakeStalwartConfiguration {
    @Bean
    @Primary
    RecordingStalwartClient recordingStalwartClient() {
      return new RecordingStalwartClient();
    }
  }

  static class RecordingStalwartClient implements StalwartClient {
    final List<String> ensuredSubjects = new ArrayList<>();
    final List<String> disabledAccounts = new ArrayList<>();
    List<String> lastAliases = List.of();

    @Override
    public String ensureAccount(
        String oauthSub, String displayName, List<String> aliases, long quotaBytes) {
      ensuredSubjects.add(oauthSub);
      lastAliases = List.copyOf(aliases);
      return "stalwart-1";
    }

    @Override
    public void setEnabled(String accountId, boolean enabled) {
      if (!enabled) {
        disabledAccounts.add(accountId);
      }
    }

    @Override
    public void updateAliases(String accountId, List<String> aliases) {
      lastAliases = List.copyOf(aliases);
    }
  }
}