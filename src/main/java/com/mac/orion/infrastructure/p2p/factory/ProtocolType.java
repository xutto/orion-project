package com.mac.orion.infrastructure.p2p.factory;

import com.mac.orion.infrastructure.p2p.protocol.FileResultsProtocol;
import com.mac.orion.infrastructure.p2p.protocol.FileSearchProtocol;
import com.mac.orion.infrastructure.p2p.protocol.FileSharerProtocol;
import com.mac.orion.infrastructure.p2p.protocol.FileTransferProtocol;
import com.mac.orion.infrastructure.p2p.protocol.KadProtocol;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProtocolType {

  KAD_PROTOCOL("kadProtocol", KadProtocolFactory.class, KadProtocol.class),
  FILE_SHARER_PROTOCOL("fileSharerProtocol", FileSharerProtocolFactory.class,
      FileSharerProtocol.class),
  FILE_SEARCH_PROTOCOL("fileSearchProtocol", FileSearchProtocolFactory.class,
      FileSearchProtocol.class),
  FILE_RESULTS_PROTOCOL("fileResultsProtocol", FileResultsProtocolFactory.class,
      FileResultsProtocol.class),
  IDENTIFY_PROTOCOL("identifyProtocol", IdentifyProtocolFactory.class,
      io.libp2p.protocol.Identify.class),
  FILE_TRANSFER_PROTOCOL("FileTransferProtocol", FileTransferProtocolFactory.class,
      FileTransferProtocol.class);

  private final String beanName;
  //  private final Class<? extends ProtocolFactory<T>> factoryClass;
  private final Class<? extends ProtocolFactory<?>> protocolFactoryClass;
  private final Class<?> protocolClass;


}
