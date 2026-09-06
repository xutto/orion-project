package com.mac.orion.infrastructure.p2p.dial;

import com.google.protobuf.ByteString;
import com.mac.orion.application.out.KadDialerUseCase;
import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Address;
import com.mac.orion.domain.model.HostNode;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.p2p.controller.KadController;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import com.mac.orion.infrastructure.p2p.model.Discovery.DiscoveryRequest;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.Peer.PeerAddress;
import identify.pb.IdentifyOuterClass;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.StrictProtocolBinding;
import io.libp2p.protocol.IdentifyController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mac.orion.domain.share.Constants.PROTOCOL_P2P;

@Slf4j
@Service
@RequiredArgsConstructor
public class KadDialerAdapter implements KadDialerUseCase {

  private final ProtocolFactoryCreator protocolFactoryCreator;
  private final Host hostNode;
  private final RoutingTable routingTable;
  private final PeerMapper peerMapper;
  private final NodeConfigUseCase nodeConfigUseCase;

  @Override
  public void sendAnnounceAndDiscovery(Peer targetPeer) {

    log.info("Caller host node: {}", hostNode.getPeerId());
    log.info("To target node: {} \n", targetPeer.getId());

    // /ip4/127.0.0.1/tcp/4001/p2p/QmXyz123...

    final Multiaddr[] receiverAddress = targetPeer.getAddress().stream()
        .map(address ->
            new Multiaddr(address.buildAddressChain() + PROTOCOL_P2P + targetPeer.getId()))
        .toArray(Multiaddr[]::new);
    final PeerId peerId = PeerId.fromBase58(targetPeer.getId());

    // ----- Multiaddr(targetPeer.getAddress() + PROTOCOL_P2P + targetPeer.getId()); -----

    final StrictProtocolBinding<IdentifyController> protocol = protocolFactoryCreator.createProtocol(
        ProtocolType.IDENTIFY_PROTOCOL);

    hostNode.getNetwork()
        .connect(peerId, receiverAddress)
        .thenApply(conn -> conn.muxerSession().createStream(protocol))
        .thenCompose(fController -> fController.getController().orTimeout(2, TimeUnit.SECONDS)
            .thenAccept(identifyController -> identifyController.id()
                .thenAccept(id -> {
                  final HostNode hostNodeData = routingTable.getHostNodeData();
                  final Address addressByIdentify = dialIdentifyToPeer(id);
                  if (addressByIdentify != null) {
                    hostNodeData.getAddresses().add(addressByIdentify);
                  }

                  // building request
                  final PeerInfoDiscovery peerInfoRequest = buildDiscoveryRequest(hostNodeData);

                  // dial to peer with an array of multiAddress
                  dialToPeerWithMultiAddress(peerId, receiverAddress, peerInfoRequest);
                })))
        .exceptionally(ex -> {
          log.warn("Peer Timeout [{}]", peerId);
          log.error("With exception: ", ex);
          routingTable.publish(OperationsType.REMOVE, targetPeer);
          return null;
        })
        .whenComplete((v, t) -> log.info("FileSharerController dialing completed", t));

  }

  private Address dialIdentifyToPeer(IdentifyOuterClass.Identify id) {
    log.info("Identify response identifyRaw: {}", id);
    try {
      log.debug("Identify response listenAddress: {}", id.getListenAddrsList().stream()
          .map(a -> Multiaddr.deserialize(a.toByteArray()))
          .collect(java.util.stream.Collectors.toList()));

      final Multiaddr observedMultiaddr = Multiaddr.deserialize(
          id.getObservedAddr().toByteArray());
      log.info("Identify response observableAddress: {}", observedMultiaddr);

      // /ip4/127.0.0.1/tcp/50503 + port obtained from the NODE_CONFIG row
      return peerMapper.mapAddressWithPort(observedMultiaddr, nodeConfigUseCase.getPort());
    } catch (Exception e) {
      log.warn("Could not resolve the ObservedAddr from the Identify response, skipping", e);
      return null;
    }
  }

  private void dialToPeerWithMultiAddress(PeerId peerId, Multiaddr[] receiverAddress,
      PeerInfoDiscovery peerInfoRequest) {

    final StrictProtocolBinding<KadController> kadProtocol = protocolFactoryCreator.createProtocol(
        ProtocolType.KAD_PROTOCOL);

    hostNode.getNetwork()
        .connect(peerId, receiverAddress)
        .thenApply(conn -> conn.muxerSession().createStream(kadProtocol))
        .thenCompose(StreamPromise::getController)
        .thenAccept(controller -> {
          controller.send(peerInfoRequest)
              .thenAccept(valid -> log.info("Message sent successfully: {}", valid))
              .exceptionally(ex -> {
                log.error("Error sending message", ex);
                return null;
              })
              .whenComplete((v, t) -> log.info("KadDialerAdapter dialing completed"));
        })
        .exceptionally(ex -> {
          log.error("Error getting controller", ex);
          return null;
        })
        .whenComplete((v, t) -> log.info("KadController dialing completed", t));
  }

  @NotNull
  private PeerInfoDiscovery buildDiscoveryRequest(HostNode hostNodeData) {
    final Set<Address> addresses = hostNodeData.getAddresses();

    final Set<PeerAddress> addressSet = addresses.stream().map(peerMapper::mapAddressFromDomain)
        .collect(Collectors.toSet());

    final DiscoveryRequest discoveryRequest = DiscoveryRequest.newBuilder()
        .setId(ByteString.copyFromUtf8(hostNodeData.getId()))
        .addAllAddress(addressSet)
        .build();
    return PeerInfoDiscovery.newBuilder()
        .setRequest(discoveryRequest).build();
  }

}
