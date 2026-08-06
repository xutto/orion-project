package com.mac.orion.infrastructure.p2p.sender;

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
public class FileSearchSender implements
    ProtocolMessageHandler<Search.SearchInfo>, FileSearchController {

  private final Stream stream;

  @Override
  public CompletableFuture<Boolean> send(Search.SearchInfo message) {
    log.debug("Sending message: {}", message);
    log.info("Sending message snippet: [ {} ]", message.getRequest().getSnippet());
    final byte[] byteArray = message.toByteArray();
    log.debug("byte-array: {}", byteArray);
    stream.writeAndFlush(message);
    stream.closeWrite();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<Discovery.PeerInfoDiscovery> sendAsync(Search.SearchInfo searchInfo) {
    //todo investigate async option
    return null;
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream sender FileSearch closed: {}", stream);
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }

  // TODO IMPORTANTE: REVISAR EL ONMESSAGE DE ESTE PROTOCOLO PUESTO QUE LA VUELTA SE HACE HACIA EL PEER QUE LO PIDE
//  @Override
//  public void onMessage(@NotNull Stream stream, Search.SearchInfo msg) {
//
//    final Discovery.DiscoveryResponse response = msg.getResponse();
//
//    log.info("---- Message received: {}", msg);
//  }
}
