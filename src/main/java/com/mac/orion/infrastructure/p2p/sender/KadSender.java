package com.mac.orion.infrastructure.p2p.sender;

import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.p2p.controller.KadController;
import com.mac.orion.infrastructure.p2p.model.Discovery.DiscoveryResponse;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
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
public class KadSender implements ProtocolMessageHandler<PeerInfoDiscovery>, KadController {

  private final Stream stream;
  private final CompletableFuture<PeerInfoDiscovery> resp = new CompletableFuture<>();
  private final KadDiscoveryUseCase kadDiscoveryProcessService;
  private final PeerMapper peerMapper;

  @Override
  public CompletableFuture<Boolean> send(PeerInfoDiscovery peerInfoDiscovery) {

    log.debug("Sending blocked message: {}", peerInfoDiscovery);

    final byte[] byteArray = peerInfoDiscovery.toByteArray();
    log.debug("byte-array: {}", byteArray);
    stream.writeAndFlush(peerInfoDiscovery);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<PeerInfoDiscovery> sendAsync(PeerInfoDiscovery peerInfoDiscovery) {
    log.debug("Sending async message FROM: {}", peerInfoDiscovery);
    stream.writeAndFlush(peerInfoDiscovery);
    return resp; // todo explorar la posibilidad de llamar al sendAsync() lo que hace que se devuelva
    //                un CompletableFuture con la respuesta del peer remoto, que puede ser controlada en el
    //                KadDialerAdapter de forma asíncrona
    //                [esto sería algo así como se manda el mensaje con ¿espera?,
    //                y en el dialer se devolvería la respuesta del peer al que se llama]
  }

  @Override
  public void onMessage(@NotNull Stream stream, PeerInfoDiscovery msg) {

    final DiscoveryResponse response = msg.getResponse();

    final Set<Peer> closestPeers = response.getClosestPeersList().stream()
        .map(peerMapper::peerDataToPeer)
        .collect(Collectors.toSet());

    kadDiscoveryProcessService.manageRoutingTable(closestPeers);
    resp.complete(msg); // todo cambiar a objeto coherente para devolver en el future NO ESTOY SEGURO DE QUE COJONES ES ESTO

    stream.closeWrite();
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream sender Kad closed: {}", stream);
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }
}
