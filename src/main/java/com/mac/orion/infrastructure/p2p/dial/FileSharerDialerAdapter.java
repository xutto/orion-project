package com.mac.orion.infrastructure.p2p.dial;

import static com.mac.orion.domain.share.Constants.PROTOCOL_P2P;

import com.mac.orion.application.out.FileSharerDialerUseCase;
import com.mac.orion.domain.dht.FileRoutingTable;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSharerController;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileManifest;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharer;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharerRequest;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileSharerDialerAdapter implements FileSharerDialerUseCase {

  private final FileRoutingTable fileRoutingTable;
  //  private final FileSharerProtocol fileSharerProtocol;
  private final ProtocolFactoryCreator protocolFactoryCreator;
  private final Host hostNode;
  private final FilesMapper filesMapper;

  @Override
  public void sendSharedFiles(Peer targetPeer) {
    final Set<File> fileRoutingTableData = fileRoutingTable.getData();

    log.info("Sending files: [{}], to peer: {}", fileRoutingTableData.size(), targetPeer.getId());

    // circuit braker
    if (fileRoutingTableData.isEmpty()) {
      return;
    }

    final FileSharer fileSharing = getFileSharer(fileRoutingTableData);

    // get controller and dial
    final Multiaddr[] receiverAddress = targetPeer.getAddress().stream()
        .map(address ->
            new Multiaddr(address.buildAddressChain() + PROTOCOL_P2P + targetPeer.getId()))
        .toArray(Multiaddr[]::new);
    final PeerId peerId = PeerId.fromBase58(targetPeer.getId());

    final StrictProtocolBinding<FileSharerController> fileSharerProtocol = protocolFactoryCreator.createProtocol(
        ProtocolType.FILE_SHARER_PROTOCOL);

    hostNode.getNetwork()
        .connect(peerId, receiverAddress)
        .thenApply(conn -> conn.muxerSession().createStream(fileSharerProtocol))
        .thenCompose(StreamPromise::getController)
        .thenAccept(controller -> {
          controller.send(fileSharing)
              .thenAccept(valid -> {
                log.info("Message sent successfully: {}", valid);
//                fileSharerController.getStream().closeWrite();
              })
              .exceptionally(ex -> {
                log.error("Error sending file sharing message", ex);
                return null;
              })
              .whenComplete((v, t) -> log.info("FileSharerController dialing completed"));
        })
        .exceptionally(ex -> {
          log.error("Error getting controller", ex);
          return null;
        })
        .whenComplete((v, t) -> log.info("FileSharerController dialing completed", t));
  }

  @NotNull
  private FileSharer getFileSharer(Set<File> data) {
    final Set<FileManifest> fileManifests = data.stream().map(filesMapper::domainToFileManifest)
        .collect(Collectors.toSet());

    final FileSharerRequest fileSharerRequest = FileSharerRequest.newBuilder()
        .addAllFilesManifest(fileManifests)
        .build();

    return FileSharer.newBuilder()
        .setRequest(fileSharerRequest)
        .build();
  }

}
