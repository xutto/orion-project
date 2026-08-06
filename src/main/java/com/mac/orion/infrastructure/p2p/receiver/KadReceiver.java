package com.mac.orion.infrastructure.p2p.receiver;

import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.p2p.controller.KadController;
import com.mac.orion.infrastructure.p2p.model.Discovery.DiscoveryResponse;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.Peer.PeerData;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Slf4j
@RequiredArgsConstructor
public class KadReceiver implements ProtocolMessageHandler<PeerInfoDiscovery>,
    KadController {

  private final KadDiscoveryUseCase kadDiscoveryProcessService;
  private final PeerMapper peerMapper;

  @Override
  public CompletableFuture<Boolean> send(PeerInfoDiscovery message) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public CompletableFuture<PeerInfoDiscovery> sendAsync(PeerInfoDiscovery message) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public void onMessage(@NotNull Stream stream, PeerInfoDiscovery msg) {

    try {
      log.debug("Message received from id: {}, and ip: {}", msg.getRequest().getId(),
          stream.getConnection().remoteAddress());

      final Peer clientPeer = peerMapper.mapPeerFromDiscoveryRequest(msg.getRequest());

      // get closest peers
      final Set<PeerData> closestPeerData = kadDiscoveryProcessService.discoverPeers(clientPeer)
          .stream()
          .map(peerMapper::peerToPeerData)
          .collect(Collectors.toSet());

      // build response
      final DiscoveryResponse discoveryResponse = DiscoveryResponse.newBuilder()
          .addAllClosestPeers(closestPeerData)
          .build();
      final PeerInfoDiscovery peerInfoDiscovery = PeerInfoDiscovery.newBuilder()
          .clearRequest()
          .setResponse(discoveryResponse)
          .build();

      log.info("Response message with: [{}] new closest peers",
          peerInfoDiscovery.getResponse().getClosestPeersCount());

      // response
      stream.writeAndFlush(peerInfoDiscovery);
      stream.closeWrite();
    } catch (Exception e) {
      log.error("Error when received MSG", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }


  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream receiver Kad closed: {}", stream);
  }

}
