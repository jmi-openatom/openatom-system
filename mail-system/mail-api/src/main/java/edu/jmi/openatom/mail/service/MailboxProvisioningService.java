package edu.jmi.openatom.mail.service;

import edu.jmi.openatom.mail.config.MailProperties;
import edu.jmi.openatom.mail.domain.MailboxAccount;
import edu.jmi.openatom.mail.domain.ProvisionRequest;
import edu.jmi.openatom.mail.domain.ProvisionResponse;
import edu.jmi.openatom.mail.repository.MailboxRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailboxProvisioningService {
  private final MailProperties properties;
  private final MailboxRepository repository;
  private final LocalPartGenerator generator;
  private final StalwartClient stalwart;

  public MailboxProvisioningService(
      MailProperties properties,
      MailboxRepository repository,
      LocalPartGenerator generator,
      StalwartClient stalwart) {
    this.properties = properties;
    this.repository = repository;
    this.generator = generator;
    this.stalwart = stalwart;
  }

  @Transactional
  public ProvisionResponse provision(ProvisionRequest request) {
    var alreadyProcessed = repository.findByEvent(request.eventId());
    if (alreadyProcessed.isPresent()) {
      return ProvisionResponse.from(alreadyProcessed.get());
    }

    repository.ensureShell(
        request.sub(),
        request.userId(),
        request.displayName(),
        properties.getDomain(),
        properties.getDefaultQuotaBytes());
    MailboxAccount account = repository.lockBySub(request.sub());

    if (shouldSuspend(request)) {
      if (account.stalwartAccountId() != null) {
        stalwart.setEnabled(account.stalwartAccountId(), false);
      }
      repository.markSuspended(account.id(), request.eventId());
      repository.recordProcessed(request.eventId(), account.id());
      return ProvisionResponse.from(repository.lockBySub(request.sub()));
    }

    if (account.localPart() == null) {
      String base = generator.baseFromName(request.displayName());
      if (base == null) {
        repository.markWaiting(account.id(), request.displayName(), request.eventId());
        repository.recordProcessed(request.eventId(), account.id());
        return ProvisionResponse.from(repository.lockBySub(request.sub()));
      }
      account = allocate(account, request.displayName(), base);
    }

    List<String> aliases = repository.aliases(account.id());
    String stalwartId =
        stalwart.ensureAccount(
            account.oauthSub(), request.displayName(), aliases, account.quotaBytes());
    repository.markActive(account.id(), stalwartId, request.eventId());
    repository.recordProcessed(request.eventId(), account.id());
    return ProvisionResponse.from(repository.lockBySub(request.sub()));
  }

  @Transactional
  public ProvisionResponse correctPrimaryAddress(String sub, String preferredLocalPart) {
    MailboxAccount account = repository.lockBySub(sub);
    if (account.stalwartAccountId() == null) {
      throw new IllegalStateException("mailbox_not_active");
    }
    String base = generator.validateManual(preferredLocalPart);
    for (int attempt = 0; attempt < 8; attempt++) {
      String candidate = generator.candidate(base, account.oauthSub(), attempt);
      String address = candidate + "@" + account.mailDomain();
      if (address.equals(account.primaryAddress())) {
        return ProvisionResponse.from(account);
      }
      if (repository.trySetPrimaryAlias(account.id(), candidate, address)) {
        stalwart.updateAliases(account.stalwartAccountId(), repository.aliases(account.id()));
        return ProvisionResponse.from(repository.lockBySub(sub));
      }
    }
    throw new IllegalStateException("mailbox_address_space_exhausted");
  }

  public ProvisionResponse status(String sub) {
    return repository
        .findBySub(sub)
        .map(ProvisionResponse::from)
        .orElseThrow(() -> new MailboxNotFoundException(sub));
  }

  private MailboxAccount allocate(MailboxAccount account, String displayName, String base) {
    for (int attempt = 0; attempt < 8; attempt++) {
      String candidate = generator.candidate(base, account.oauthSub(), attempt);
      String address = candidate + "@" + account.mailDomain();
      if (repository.assignAddress(account.id(), displayName, candidate, address)) {
        return repository.lockBySub(account.oauthSub());
      }
    }
    throw new IllegalStateException("mailbox_address_space_exhausted");
  }

  private boolean shouldSuspend(ProvisionRequest request) {
    return "USER_DELETION_REQUESTED".equals(request.eventType())
        || "DISABLED".equals(request.status())
        || "LOCKED".equals(request.status());
  }
}
