package com.mac.orion.infrastructure.p2p.factory;

import io.libp2p.core.multistream.StrictProtocolBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

@Slf4j
//@Component
@RequiredArgsConstructor
public class ProtocolFactoryCreator {

  //  private final Map<String, ProtocolFactory<?>> protocolFactories;
  private final ApplicationContext applicationContext;

  public <C> StrictProtocolBinding<C> createProtocol(
      ProtocolType protocolType /*Class<T> clazz*/) {

    ProtocolFactory<?> rawFactory = applicationContext.getBean(
        protocolType.getProtocolFactoryClass());
    StrictProtocolBinding<?> protocol = rawFactory.getProtocol();

    if (protocolType.getProtocolClass() != null && !protocolType.getProtocolClass()
        .isInstance(protocol)) {
      throw new ClassCastException("Factory " + rawFactory.getClass().getSimpleName() +
          " returns " + protocol.getClass().getName() + " but expected "
          + protocolType.getProtocolClass().getName());
    }
    return (StrictProtocolBinding<C>) protocol;
  }

//  private void theprueba() {
//    final StrictProtocolBinding<KadController> protocol = createProtocolFactory(
//        ProtocolType.KAD_PROTOCOL);
//
//
//  }
}
