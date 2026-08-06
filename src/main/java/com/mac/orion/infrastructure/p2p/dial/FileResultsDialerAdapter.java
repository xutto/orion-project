package com.mac.orion.infrastructure.p2p.dial;


import static com.mac.orion.domain.share.Constants.PROTOCOL_P2P;

import com.mac.orion.application.out.FileResultsDialerUeCase;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.SearchResult;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileResultsController;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchFilesResult;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchInfoResult;
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
public class FileResultsDialerAdapter implements
    FileResultsDialerUeCase {

  private final Host hostNode;
//  private final FileResultsProtocol fileResultsProtocol;
  private final ProtocolFactoryCreator protocolFactoryCreator;
  private final SearchMapper searchMapper;

  @Override
  public void sendFileSearchResults(SearchResult searchResult, Peer finderPeer) {

    log.info("Sending result files: [{}], to peer: {}", searchResult.getFiles().size(), finderPeer.getId());

    // get controller and dial
    final Multiaddr[] receiverAddress = finderPeer.getAddress().stream()
        .map(address ->
            new Multiaddr(address.buildAddressChain() + PROTOCOL_P2P + finderPeer.getId()))
        .toArray(Multiaddr[]::new);

    final PeerId peerId = PeerId.fromBase58(finderPeer.getId());
    final SearchFilesResult request = searchMapper.mapSearchFilesResultFromSearchResource(
        searchResult);
    final SearchInfoResult searchInfoResult = SearchInfoResult.newBuilder().setRequest(request)
        .build();

    final StrictProtocolBinding<FileResultsController> protocol = protocolFactoryCreator.createProtocol(
        ProtocolType.FILE_RESULTS_PROTOCOL);


    hostNode.getNetwork()
        .connect(peerId, receiverAddress)
        .thenApply(conn -> conn.muxerSession().createStream(protocol))
        .thenCompose(StreamPromise::getController)
        .thenAccept(fileSharerController -> {
          fileSharerController.send(searchInfoResult)
              .thenAccept(valid -> log.info("Message sent successfully: {}", valid))
              .exceptionally(ex -> {
                log.error("Error sending message", ex);
                return null;
              })
              .whenComplete((v, t) -> log.info("FileResults dialing completed"));
        })
        .exceptionally(ex -> {
          log.error("Error getting controller", ex);
          return null;
        })
        .whenComplete((v, t) -> log.info("FileResults dialing completed", t));
  }

}
