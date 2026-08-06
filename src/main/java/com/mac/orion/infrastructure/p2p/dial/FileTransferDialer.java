package com.mac.orion.infrastructure.p2p.dial;


import static com.mac.orion.domain.share.Constants.PROTOCOL_P2P;

import com.mac.orion.application.out.FileTransferDialerUseCase;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileTransferController;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import com.mac.orion.infrastructure.p2p.model.FileTransfer;
import com.mac.orion.infrastructure.p2p.model.FileTransfer.FileMessage;
import com.mac.orion.infrastructure.p2p.model.FileTransfer.MessageType;
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
public class FileTransferDialer implements FileTransferDialerUseCase {

  private final FilesMapper filesMapper;
  private final Host hostNode;
  private final ProtocolFactoryCreator protocolFactoryCreator;

  @Override
  public void sendInitialDownloadFileProcess(Hash hash, Peer targetPeer) {

    // get a target connection chain
    final Multiaddr[] receiverAddress = targetPeer.getAddress().stream()
        .map(address ->
            new Multiaddr(address.buildAddressChain() + PROTOCOL_P2P + targetPeer.getId()))
        .toArray(Multiaddr[]::new);
    final PeerId peerId = PeerId.fromBase58(targetPeer.getId());

    // map to request
    final FileTransfer.Hash hashTransfer = filesMapper.hashToHashTransfer(hash);
    final FileMessage fileMessageRequest = FileMessage.newBuilder()
        .setMessageType(MessageType.FILE_GET).setHash(hashTransfer).build();

    // create protocol
    final StrictProtocolBinding<FileTransferController> protocol = protocolFactoryCreator.createProtocol(
        ProtocolType.FILE_TRANSFER_PROTOCOL);

    hostNode.getNetwork()
        .connect(peerId, receiverAddress)
        .thenApply(conn -> conn.muxerSession().createStream(protocol))
        .thenCompose(StreamPromise::getController)
        .thenAccept(controller -> {
          controller.send(fileMessageRequest) // todo con sobrecarga de metodo igual se puede aproximar una unificación de todos los dialers, manteniendo esta estructura
              .thenAccept(valid -> {
                log.info("Message sent successfully: {}", valid);
              })
              .exceptionally(ex -> {
                log.error("Error sending file transfer message", ex);
                return null;
              })
              .whenComplete((v, t) -> log.info("FileTransferController dialing completed"));
        })
        .exceptionally(ex -> {
          log.error("Error getting controller", ex);
          return null;
        })
        .whenComplete((v, t) -> log.info("FileTransferController dialing completed", t));
  }
}
