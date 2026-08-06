package com.mac.orion.infrastructure.p2p.controller;

import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import java.util.concurrent.CompletableFuture;

// todo esta interfaz podría ser mutable y tener diferentes implementaciones dependiendo del mensaje proto que quiera mandarse
public interface KadController extends Controller {


  CompletableFuture<Boolean> send(PeerInfoDiscovery message);

  CompletableFuture<PeerInfoDiscovery> sendAsync(PeerInfoDiscovery message);


}
