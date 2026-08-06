package com.mac.orion.application.in;

import com.mac.orion.domain.model.Peer;
import java.util.Set;

public interface DiscoveryResponderUseCase {

  Set<Peer> getClosestPeers(String target);

//  String getGuestAddress(String target);
}
