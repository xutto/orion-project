package com.mac.orion.infrastructure.p2p.controller;

import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharer;
import java.util.concurrent.CompletableFuture;

public interface FileSharerController extends Controller {


  CompletableFuture<Boolean> send(FileSharer message);

  CompletableFuture<PeerInfoDiscovery> sendAsync(FileSharer message);

//  Stream getStream();

}
