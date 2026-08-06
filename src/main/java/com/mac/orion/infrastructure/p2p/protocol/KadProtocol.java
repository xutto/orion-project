package com.mac.orion.infrastructure.p2p.protocol;

import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.p2p.controller.KadController;
import com.mac.orion.infrastructure.p2p.handler.KadProtobufMessageHandler;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KadProtocol extends StrictProtocolBinding<KadController> {

  public static final String PROTOCOL_CHANNEL_DISCOVERY_SELF_1_0_0 = "/discovery-self/1.0.0";
  public static final String KAD_PROTOCOL_ID = "kadProtocol";

  public KadProtocol(KadDiscoveryUseCase kadDiscoveryProcessService,
      PeerMapper peerMapper) {
    super(KAD_PROTOCOL_ID,
        new KadProtobufMessageHandler(kadDiscoveryProcessService, peerMapper));
  }

  @NotNull
  @Override
  public ProtocolDescriptor getProtocolDescriptor() {
    return new ProtocolDescriptor(
        List.of(PROTOCOL_CHANNEL_DISCOVERY_SELF_1_0_0)); // todo configurable
  }
}
