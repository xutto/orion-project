package com.mac.orion.infrastructure.p2p.dial;

import com.google.protobuf.MessageLite;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import io.libp2p.core.multiformats.Multiaddr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Component
@RequiredArgsConstructor
public class DialerManager {

//  private final ProtocolFactoryCreator protocolFactoryCreator;
//  private final Host hostNode;

  public void dialWithConnection(String peerRemoteId, Multiaddr[] receiverAddress, MessageLite requestMessage, ProtocolType protocolType) {

//    if (requestMessage instanceof FileSharer){
//      final FileSharer request = (FileSharer) requestMessage;
//    }

//    final StrictProtocolBinding<KadController> kadProtocol = protocolFactoryCreator.createProtocolFactory(
//        protocolType);
//
//    hostNode.getNetwork()
//        .connect(peerRemoteId, receiverAddress)
//        .thenApply(conn -> conn.muxerSession().createStream(kadProtocol))
//        .thenCompose(StreamPromise::getController)
//        .thenAccept(fileSharerController -> {
//          fileSharerController.send(protocolType)
//              .thenAccept(valid -> log.info("Message sent successfully: {}", valid))
//              .exceptionally(ex -> {
//                log.error("Error sending file sharing message", ex);
//                return null;
//              })
//              .whenComplete((v, t) -> log.info("FileSharerController dialing completed"));
//        })
//        .exceptionally(ex -> {
//          log.error("Error getting file sharing controller", ex);
//          return null;
//        })
//        .whenComplete((v, t) -> log.info("KadController dialing completed", t));

  }

}
