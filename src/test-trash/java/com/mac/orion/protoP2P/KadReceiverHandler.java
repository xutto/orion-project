package com.mac.orion.protoP2P;

import com.mac.orion.infrastructure.p2p.model.PeerAnnounce.PeerInfoRequest;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Slf4j
public class KadReceiverHandler implements ProtocolMessageHandler<PeerInfoRequest>, KadController {

  @Override
  public CompletableFuture<Boolean> send(PeerInfoRequest message) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public CompletableFuture<PeerInfoRequest> sendAsync(PeerInfoRequest message) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public void onMessage(@NotNull Stream stream, PeerInfoRequest msg) {
    log.info("Message received: {}", msg);
  }

  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream closed: {}", stream);
  }

}
