package com.mac.orion.infrastructure.p2p.config;

import static com.mac.orion.domain.dht.OperationsType.REMOVE;
import static com.mac.orion.domain.dht.OperationsType.SAVE;

import com.google.common.base.Strings;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.dht.RoutingDataHashTable;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Address;
import com.mac.orion.domain.model.HostNode;
import com.mac.orion.domain.model.Peer;
import io.libp2p.core.Host;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingTableConfiguration {

  public static final String ADDRESS_TYPE_IP_4 = "ip4";
  public static final String TRANSMISSION_TCP = "tcp";
  @Value("${orion.p2p.bootstrap-ip}")
  private String bootstrapAddress;
  @Value("${orion.p2p.bootstrap-port}")
  private Integer bootstrapPort;
  @Value("${orion.p2p.bootstrap-id}")
  private String bootstrapId;


  @Bean
  public RoutingTable routingTable(Host hostNode) {

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

    if (!Strings.isNullOrEmpty(bootstrapId)) {
      // todo bootstrap address must be in DB
      final Address bootStrapAddress = Address.builder()
          .type(ADDRESS_TYPE_IP_4)
          .transmission(TRANSMISSION_TCP)
          .port(bootstrapPort)
          .ip(bootstrapAddress)
          .build();
      final Set<Address> addresses = new HashSet<>();
      addresses.add(bootStrapAddress);
      final Peer bootstrapPeer = Peer.builder().id(bootstrapId).address(addresses).build();
      routingDataHashTable.publish(SAVE, bootstrapPeer);
    }

    return routingDataHashTable;
  }

}
