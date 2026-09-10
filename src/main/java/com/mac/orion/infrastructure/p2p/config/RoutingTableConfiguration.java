package com.mac.orion.infrastructure.p2p.config;

import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.dht.RoutingDataHashTable;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Address;
import com.mac.orion.domain.model.HostNode;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.settings.Bootstrap;
import io.libp2p.core.Host;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

import static com.mac.orion.domain.dht.OperationsType.REMOVE;
import static com.mac.orion.domain.dht.OperationsType.SAVE;

/**
 * Seeds the routing table at startup from the BOOTSTRAP table (database, single source of truth).
 * The table is populated either by the start script (SQL) or interactively from the UI.
 * Ephemeral peers (discovered by KAD) are NOT involved: they live only in
 * memory and are never read or written here.
 */
@Configuration
@Slf4j
public class RoutingTableConfiguration {

  private static final String ADDRESS_TYPE_IP_4 = "ip4";
  private static final String TRANSMISSION_TCP = "tcp";

  @Bean
  public RoutingTable routingTable(Host hostNode, BootstrapUseCase bootstrapUseCase) {

    // initializing hostNodeData without Addresses because not know now.
    final HostNode ownHostNodeData = HostNode.builder()
        .id(hostNode.getPeerId().toString())
        .addresses(new HashSet<>())
        .build();
    final HashSet<OperationsType> operations = new HashSet<>();
    operations.add(SAVE);
    operations.add(REMOVE);
    final RoutingDataHashTable routingDataHashTable = RoutingDataHashTable.create(ownHostNodeData,
        operations);

    // Seed the routing table from the BOOTSTRAP table in the DB.
    final Set<Bootstrap> bootstraps = new HashSet<>(bootstrapUseCase.findAll());
    for (final Bootstrap bootstrap : bootstraps) {
      final Integer port = parsePort(bootstrap.port());
      if (port == null) {
        log.warn("Skipping bootstrap with an invalid port ({}): {}:{}", bootstrap.port(),
            bootstrap.ip(), bootstrap.id());
        continue;
      }
      final Address bootStrapAddress = Address.builder()
          .type(ADDRESS_TYPE_IP_4)
          .transmission(TRANSMISSION_TCP)
          .port(port)
          .ip(bootstrap.ip())
          .build();
      final Peer bootstrapPeer = Peer.builder().id(bootstrap.id()).address(Set.of(bootStrapAddress))
          .build();
      routingDataHashTable.publish(SAVE, bootstrapPeer);
      log.info("Routing table seeded with bootstrap: {}:{} (id={})", bootstrap.ip(), port,
          bootstrap.id());
    }

    return routingDataHashTable;
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
