package com.mac.orion.infrastructure.p2p.factory;

import io.libp2p.core.multistream.StrictProtocolBinding;

public interface ProtocolFactory<C> {

  StrictProtocolBinding<C> getProtocol();

}
