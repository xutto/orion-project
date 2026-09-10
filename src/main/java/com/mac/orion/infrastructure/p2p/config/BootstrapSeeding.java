package com.mac.orion.infrastructure.p2p.config;

import com.google.common.base.Strings;
import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.domain.model.settings.Bootstrap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * One-shot, idempotent seeding of the BOOTSTRAP table from the legacy yml values
 * ({@code orion.p2p.bootstrap-*}). Runs on startup, before the routing table is seeded from the DB:
 * if the table is still empty and the yml carries a valid bootstrap, that single bootstrap is
 * migrated into the DB so the current configuration is not lost. Once the DB holds bootstraps the
 * yml is never read again (the DB is the single source of truth).
 * <p>
 * Idempotent: re-running finds the table already non-empty and does nothing.
 */
@Slf4j
@Component
public class BootstrapSeeding {

  private final BootstrapUseCase bootstrapUseCase;

  @Value("${orion.p2p.bootstrap-ip:}")
  private String ymlIp;
  @Value("${orion.p2p.bootstrap-port:}")
  private String ymlPort;
  @Value("${orion.p2p.bootstrap-id:}")
  private String ymlId;

  public BootstrapSeeding(BootstrapUseCase bootstrapUseCase) {
    this.bootstrapUseCase = bootstrapUseCase;
  }

  public void seedFromYmlIfEmpty() {
    if (!bootstrapUseCase.findAll().isEmpty()) {
      return; // already migrated (or the user manages bootstraps): nothing to do
    }
    final String ip = ymlIp == null ? "" : ymlIp.trim();
    final String port = ymlPort == null ? "" : ymlPort.trim();
    final String id = ymlId == null ? "" : ymlId.trim();
    if (Strings.isNullOrEmpty(ip) || Strings.isNullOrEmpty(port) || Strings.isNullOrEmpty(id)
        || parsePort(port) == null) {
      return; // yml has no valid bootstrap: nothing to migrate
    }
    bootstrapUseCase.save(new Bootstrap(ip, port, id));
    log.info("Seeded bootstrap from yml (one-shot): {}:{} (id={})", ip, port, id);
  }

  private Integer parsePort(String rawPort) {
    try {
      final int port = Integer.parseInt(rawPort);
      return (port >= 1 && port <= 65535) ? port : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

}
