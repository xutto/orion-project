package com.mac.orion.application.in;

import com.mac.orion.domain.model.Peer;
import java.util.Set;

public interface UpdateRoutingTableUseCase {

  void addGuestPeerToRoutingTable(Peer peer);

  void manageRoutingTable(Set<Peer> newPeers);

}
