package com.mac.orion.integration.config;

import com.mac.orion.integration.handler.FileInfoAvailabilityHandler;
import com.mac.orion.integration.protocol.FileInfoAvailabilityProtocol;
import io.libp2p.core.Host;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.protocol.Ping;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class HostsConfig {

    @Bean
    public FileInfoAvailabilityProtocol fileInfoAvailabilityProtocol01(FileInfoAvailabilityHandler fileInfoAvailabilityHandler01) {
        return new FileInfoAvailabilityProtocol(fileInfoAvailabilityHandler01);
    }

    @Bean
    public FileInfoAvailabilityProtocol fileInfoAvailabilityProtocol02(FileInfoAvailabilityHandler fileInfoAvailabilityHandler02) {
        return new FileInfoAvailabilityProtocol(fileInfoAvailabilityHandler02);
    }

    @Bean
    public FileInfoAvailabilityHandler fileInfoAvailabilityHandler01() {
        return new FileInfoAvailabilityHandler();
    }

    @Bean
    public FileInfoAvailabilityHandler fileInfoAvailabilityHandler02() {
        return new FileInfoAvailabilityHandler();
    }

//    @Bean
//    public SegmentPositionProtocol fileInfoAvailabilityProtocol01(Host hostNode01, SegmentPositionHandler segmentPositionHandler) {
//        return new SegmentPositionProtocol(hostNode01, segmentPositionHandler);
//    }
//
//    @Bean
//    public FileInfoAvailabilityProtocol fileInfoAvailabilityProtocol02(Host hostNode02, FileInfoAvailabilityHandler fileInfoAvailabilityHandler02) {
//        return new FileInfoAvailabilityProtocol(hostNode02, fileInfoAvailabilityHandler02);
//    }
//
//    @Bean
//    public FileInfoAvailabilityHandler fileInfoAvailabilityHandler01() {
//        return new FileInfoAvailabilityHandler();
//    }
//
//    @Bean
//    public FileInfoAvailabilityHandler fileInfoAvailabilityHandler02() {
//        return new FileInfoAvailabilityHandler();
//    }


    @Bean
    public Host hostNode01(FileInfoAvailabilityProtocol fileInfoAvailabilityProtocol01) {
        Host hostNode01 = new HostBuilder()
                .protocol(new Ping())
                .transport(TcpTransport::new)
                .secureChannel(NoiseXXSecureChannel::new)
                .muxer(StreamMuxerProtocol::getMplex)
                .listen("/ip4/127.0.0.1/tcp/4005") // Escucha en el puerto 4001
                .build();

        hostNode01.addProtocolHandler(fileInfoAvailabilityProtocol01);
//        hostNode01.addProtocolHandler();
        hostNode01.start();
        return hostNode01;
    }

    @Bean
    public Host hostNode02(FileInfoAvailabilityProtocol fileInfoAvailabilityProtocol02) {
        Host hostNode01 = new HostBuilder()
                .protocol(new Ping())
                .transport(TcpTransport::new)
                .secureChannel(NoiseXXSecureChannel::new)
                .muxer(StreamMuxerProtocol::getMplex)
                .listen("/ip4/127.0.0.1/tcp/4005") // Escucha en el puerto 4001
                .build();

        hostNode01.addProtocolHandler(fileInfoAvailabilityProtocol02);
//        hostNode01.addProtocolHandler();
        hostNode01.start();
        return hostNode01;
    }


}
