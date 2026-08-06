package com.mac.orion.infrastructure.p2p.receiver;

import com.mac.orion.application.service.publisher.SearchReceiverPublisherService;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.SearchResource;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSearchController;
import com.mac.orion.infrastructure.p2p.model.Discovery;
import com.mac.orion.infrastructure.p2p.model.Search;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileSearchReceiver implements ProtocolMessageHandler<Search.SearchInfo>, FileSearchController {

  private final SearchReceiverPublisherService searchReceiverPublisherService;
  private final SearchMapper searchMapper;

  @Override
  public CompletableFuture<Boolean> send(Search.SearchInfo searchInfo) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public CompletableFuture<Discovery.PeerInfoDiscovery> sendAsync(Search.SearchInfo searchInfo) {
    throw new IllegalStateException("Responder only!");
  }


  @Override
  public void onMessage(@NotNull Stream stream, Search.SearchInfo msg) {

    final Search.SearchInfoRequest request = msg.getRequest();
    final SearchResource searchResource = searchMapper.mapSearchResourceFromSearchInfo(request);

    // publish request to propagate - THIS IS A bridge TO SearchPropagateService
    searchReceiverPublisherService.publish(OperationsType.RECEIVER, searchResource);

    stream.closeWrite();
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream receiver FileSearch closed: {}", stream);
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }
}
