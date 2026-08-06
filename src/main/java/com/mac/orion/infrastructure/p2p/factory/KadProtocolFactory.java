package com.mac.orion.infrastructure.p2p.factory;

import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.p2p.controller.KadController;
import com.mac.orion.infrastructure.p2p.protocol.KadProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KadProtocolFactory implements ProtocolFactory<KadController>{

  private final KadDiscoveryUseCase kadDiscoveryProcessService;
  private final PeerMapper peerMapper;

  @Override
  public KadProtocol getProtocol() {
    return new KadProtocol(kadDiscoveryProcessService, peerMapper);
  }
}
