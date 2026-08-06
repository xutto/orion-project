package com.mac.orion.application.service;

import static com.mac.orion.domain.dht.OperationsType.SAVE;

import com.mac.orion.application.in.UpdateRoutingTableUseCase;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Peer;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UpdateRoutingTableService implements UpdateRoutingTableUseCase {

  /*
      This class is needed to update the routing table in the FUTURE. Cannot be deprecated
   */

  private final RoutingTable routingDataHashTable;

  @Override
  public void addGuestPeerToRoutingTable(Peer peer) {
    routingDataHashTable.publish(SAVE, peer);
  }

  @Override
  public void manageRoutingTable(Set<Peer> newPeers) {
    log.info("manageRoutingTable: [{}] peers", newPeers.size());
    newPeers.forEach(p -> routingDataHashTable.publish(SAVE, p));
  }
}
