package com.mac.orion.protoP2P;

import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KadProtocol extends StrictProtocolBinding<KadController> {

  public static final String PROTOCOL_CHANNEL_DISCOVERY_SELF_1_0_0 = "/discovery-self/1.0.0";

  public KadProtocol(@NotNull String channel) {
    super(channel, new KadProtobufMessageHandler());
  }

  @NotNull
  @Override
  public ProtocolDescriptor getProtocolDescriptor() {
    return new ProtocolDescriptor(
        List.of(PROTOCOL_CHANNEL_DISCOVERY_SELF_1_0_0)); // todo configurable
  }
}
