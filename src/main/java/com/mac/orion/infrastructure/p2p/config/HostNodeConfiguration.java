package com.mac.orion.infrastructure.p2p.config;

import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.infrastructure.p2p.factory.ProtocolFactoryCreator;
import com.mac.orion.infrastructure.p2p.factory.ProtocolType;
import io.libp2p.core.Host;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.protocol.Ping;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Slf4j
@Configuration
public class HostNodeConfiguration {

  public static final String STRING_CONNECTION_PEER = "/ip4/0.0.0.0/tcp/";
//  public static final String STRING_CONNECTION_PEER = "/ip4/127.0.0.1/tcp/";

//  @Value("${orion.p2p.port}")
//  private String listenAddressPort;


  @Bean
  @Order(0)
  public Host hostNode(Settings settings)
      throws ExecutionException, InterruptedException {

    log.info("Initializing P2P hostNode with listen address: {}", settings.getP2p().client().port());

    return new HostBuilder()
        .protocol(
            new Ping()
        )
        .transport(TcpTransport::new)
        .secureChannel(NoiseXXSecureChannel::new)
        .muxer(StreamMuxerProtocol::getMplex)
        .listen(STRING_CONNECTION_PEER + settings.getP2p().client().port())
        .build();
  }

  @Bean
  public ProtocolFactoryCreator protocolFactoryCreator(Host hostNode, ApplicationContext context, Settings settings)
      throws ExecutionException, InterruptedException {
    final ProtocolFactoryCreator protocolFactoryCreator = new ProtocolFactoryCreator(context);
    Arrays.stream(ProtocolType.values()).forEach(protocolType ->
        hostNode.addProtocolHandler(protocolFactoryCreator.createProtocol(protocolType)));

    // todo START hostNode when finalize beans configurations
    hostNode.start().get();
    log.info("\n\nHostNode started with id: [{}] - port: [{}] \n\n", hostNode.getPeerId(), settings.getP2p().client().port());
    return protocolFactoryCreator;
  }

}
