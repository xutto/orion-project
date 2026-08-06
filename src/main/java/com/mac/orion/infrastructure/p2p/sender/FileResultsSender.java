package com.mac.orion.infrastructure.p2p.sender;


import com.mac.orion.infrastructure.p2p.controller.FileResultsController;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchInfoResult;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileResultsSender implements ProtocolMessageHandler<SearchInfoResult>,
    FileResultsController {

  private final Stream stream;

  @Override
  public CompletableFuture<Boolean> send(SearchInfoResult message) {
    log.debug("Sending message: {}", message);
    final byte[] byteArray = message.toByteArray();
    log.debug("byte-array: {}", byteArray);
    stream.writeAndFlush(message);
    stream.closeWrite();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<PeerInfoDiscovery> sendAsync(SearchInfoResult searchInfo) {
    //todo investigate async option
    return null;
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream sender FileResults closed: {}", stream);
  }


}
