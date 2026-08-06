package com.mac.orion.infrastructure.p2p.dial;

import static com.mac.orion.domain.share.Constants.PROTOCOL_P2P;

import com.mac.orion.application.out.FileSearcherDialerUseCase;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.SearchResource;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSearchController;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import com.mac.orion.infrastructure.p2p.model.Search.SearchInfo;
import com.mac.orion.infrastructure.p2p.model.Search.SearchInfoRequest;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSearchDialerAdapter implements FileSearcherDialerUseCase {

  private final Host hostNode;
  private final ProtocolFactoryCreator protocolFactoryCreator;
  private final SearchMapper searchMapper;

  @Override
  public void sendSearchFile(SearchResource searchResource, Peer targetPeer) {

    log.info("Sending search term-file: {} to peer: {}", searchResource.getSnippet(),
        targetPeer.toString());

    // get controller and dial
    final Multiaddr[] receiverAddress = targetPeer.getAddress().stream()
        .map(address ->
            new Multiaddr(address.buildAddressChain() + PROTOCOL_P2P + targetPeer.getId()))
        .toArray(Multiaddr[]::new);

    // build request
    final SearchInfoRequest request = searchMapper.mapSearchInfoFromSearchResource(searchResource);
    final SearchInfo searchInfo = SearchInfo.newBuilder().setRequest(request).build();

    final PeerId peerId = PeerId.fromBase58(targetPeer.getId());

    final StrictProtocolBinding<FileSearchController> fileSearchProtocol = protocolFactoryCreator.createProtocol(
        ProtocolType.FILE_SEARCH_PROTOCOL);

    hostNode.getNetwork()
        .connect(peerId, receiverAddress)
        .thenApply(conn -> conn.muxerSession().createStream(fileSearchProtocol))
        .thenCompose(StreamPromise::getController)
        .thenAccept(fileSharerController -> {
          fileSharerController.send(searchInfo)
              .thenAccept(valid -> log.info("Message sent successfully: {}", valid))
              .exceptionally(ex -> {
                log.error("Error sending message", ex);
                return null;
              })
              .whenComplete((v, t) -> log.info("FileSearch dialing completed"));
        })
        .exceptionally(ex -> {
          log.error("Error getting controller", ex);
          return null;
        })
        .whenComplete((v, t) -> log.info("FileSearchController dialing completed", t));


  }
}
