package com.mac.orion.protoP2P;

import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import io.libp2p.core.Host;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostCompositionSupport {

//  private KadProtocolFactory kadProtocolFactory;
  private Host hostNode;
  private RoutingDataHashTableToTesting routingTable;
  private String port;
//  private FileSharerProtocolFactory fileSharerProtocolFactory;
  private FileRoutingDataHashTableToTest fileRoutingTable;
  private ProtocolFactoryCreator protocolFactoryCreator;

}
