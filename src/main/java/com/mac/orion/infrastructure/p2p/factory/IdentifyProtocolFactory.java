package com.mac.orion.infrastructure.p2p.factory;

import io.libp2p.protocol.Identify;
import io.libp2p.protocol.IdentifyController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IdentifyProtocolFactory implements ProtocolFactory<IdentifyController> {

  @Override
  public Identify getProtocol() {
    return new Identify();
  }
}
