package com.mac.orion.application.service;

import com.mac.orion.application.in.DiscoveryResponderUseCase;
import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.application.in.UpdateRoutingTableUseCase;
import com.mac.orion.domain.model.Peer;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KadDiscoveryProcessService implements KadDiscoveryUseCase {

  private final DiscoveryResponderUseCase discoveryResponderUseCase;
  private final UpdateRoutingTableUseCase updateRoutingTableUseCase;

  @Override
  public Set<Peer> discoverPeers(Peer clientPeer) {

    // added caller peer to the routing table
    updateRoutingTableUseCase.addGuestPeerToRoutingTable(clientPeer);

    // get closest peers
    return discoveryResponderUseCase.getClosestPeers(clientPeer.getId());
  }

  @Override
  public void manageRoutingTable(Set<Peer> newPeers) {
    updateRoutingTableUseCase.manageRoutingTable(newPeers);
  }


}
