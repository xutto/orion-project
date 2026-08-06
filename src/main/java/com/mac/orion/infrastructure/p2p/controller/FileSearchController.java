package com.mac.orion.infrastructure.p2p.controller;

import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.Search.SearchInfo;
import java.util.concurrent.CompletableFuture;

public interface FileSearchController extends Controller{

  CompletableFuture<Boolean> send(SearchInfo searchInfo); // todo pass a proto object

  CompletableFuture<PeerInfoDiscovery> sendAsync(SearchInfo searchInfo);

}
