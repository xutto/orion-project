package com.mac.orion.protoP2P;

import com.mac.orion.infrastructure.p2p.model.PeerAnnounce.PeerInfoRequest;
import java.util.concurrent.CompletableFuture;

public interface KadController {

  CompletableFuture<Boolean> send(PeerInfoRequest message);

  CompletableFuture<PeerInfoRequest> sendAsync(PeerInfoRequest message);


}
