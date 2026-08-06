package com.mac.orion.protoP2P;

import com.mac.orion.infrastructure.p2p.model.PeerAnnounce.PeerInfoRequest;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class KadSenderHandler implements ProtocolMessageHandler<PeerInfoRequest>, KadController {

  private final Stream stream;
  private final CompletableFuture<PeerInfoRequest> resp = new CompletableFuture<>();

  @Override
  public CompletableFuture<Boolean> send(PeerInfoRequest peerInfoRequest) {
    log.info("Sending blocked message: {}", peerInfoRequest);
    final byte[] byteArray = peerInfoRequest.toByteArray();
    log.debug("--- byte-array: {}", byteArray);
    stream.writeAndFlush(peerInfoRequest);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<PeerInfoRequest> sendAsync(PeerInfoRequest peerInfoRequest) {
    log.info("Sending message: {}", peerInfoRequest);
    stream.writeAndFlush(peerInfoRequest);
    return resp;
  }

  @Override
  public void onMessage(@NotNull Stream stream, PeerInfoRequest msg) {
    log.info("response on KadSenderHandler: {}", msg);
    resp.complete(msg);
    stream.closeWrite();
  }
}
