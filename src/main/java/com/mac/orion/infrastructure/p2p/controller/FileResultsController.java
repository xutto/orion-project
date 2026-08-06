package com.mac.orion.infrastructure.p2p.controller;

import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchInfoResult;
import java.util.concurrent.CompletableFuture;

public interface FileResultsController extends Controller{

  CompletableFuture<Boolean> send(SearchInfoResult searchInfoResult);

  CompletableFuture<PeerInfoDiscovery> sendAsync(SearchInfoResult searchInfoResult);

}
