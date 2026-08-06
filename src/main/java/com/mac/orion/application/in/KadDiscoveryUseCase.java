package com.mac.orion.application.in;

import com.mac.orion.domain.model.Peer;
import java.util.Set;

public interface KadDiscoveryUseCase {

  Set<Peer> discoverPeers(Peer clientPeer);

  void manageRoutingTable(Set<Peer> newPeers);

}
