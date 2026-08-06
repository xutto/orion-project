package com.mac.orion.hardcorP2P;

import com.mac.orion.BaseIntTest;
import io.libp2p.core.Connection;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.protocol.Ping;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class DefinitiveIntegrationLaboratoryP2pTest extends BaseIntTest {

    public static final String PROTOCOL_P2P = "/p2p/";
    public static final String STRING_CONNECTION_RECEIVER_PEER = "/ip4/127.0.0.1/tcp/4006";
    public static final String STRING_CONNECTION_SENDER_PEER = "/ip4/127.0.0.1/tcp/4002";

    private Host getNodeReceiverGood() throws ExecutionException, InterruptedException {
        Host hostNodeBReceiver = new HostBuilder()
                .protocol(new Ping())
                .transport(TcpTransport::new)
                .secureChannel(NoiseXXSecureChannel::new)
                .muxer(StreamMuxerProtocol::getMplex)
                .listen(STRING_CONNECTION_RECEIVER_PEER) // Escucha en el puerto 4001
                .build();

        hostNodeBReceiver.addProtocolHandler(
            new ProtocolRequest01(new MessageHandlerImplNode01(hostNodeBReceiver)));
        hostNodeBReceiver.addProtocolHandler(new ProtocolRequest02(hostNodeBReceiver));
        hostNodeBReceiver.start().get();
        return hostNodeBReceiver;
    }

    private Host getNodeSenderGood() throws ExecutionException, InterruptedException {

        Host hostNodeSenderGood = new HostBuilder()
                .protocol(new Ping())
                .transport(TcpTransport::new)
                .secureChannel(NoiseXXSecureChannel::new)
                .muxer(StreamMuxerProtocol::getMplex)
                .listen(STRING_CONNECTION_SENDER_PEER) // Escucha en el puerto 4001
                .build();
        // todo [ProtocolDescriptor]!!! URGENTE MUCHO CUIDADO COLISIONA CON EL PROTOCOLO DEL SENDER
        hostNodeSenderGood.addProtocolHandler(
            new ProtocolRequest01(new MessageHandlerImplNode01(hostNodeSenderGood)));
        hostNodeSenderGood.addProtocolHandler(new ProtocolRequest02(hostNodeSenderGood));
        hostNodeSenderGood.start().get();
        return hostNodeSenderGood;

    }

    @Test
    void connectionPeersTest() throws InterruptedException, ExecutionException {

        final Host peer01 = getNodeReceiverGood();
        final Host peer02 = getNodeSenderGood();

        final PeerId peer02Id = peer02.getPeerId();
        Multiaddr receiverPeerIdBootstrapAddr = new Multiaddr(STRING_CONNECTION_SENDER_PEER + PROTOCOL_P2P + peer02Id);
        CompletableFuture<Connection> connectionFuture = peer01.getNetwork().connect(receiverPeerIdBootstrapAddr);
        connectionFuture.thenAccept(connection -> {
            log.info("Conectado al nodo bootstrap!");
        });

        Thread.sleep(5000);

    }

    @Test
        // fixme se desactiva para que no se ejecute en el build , pero es el bueno
    void transferFileProcessTestAndFrustrated() throws InterruptedException, ExecutionException {

        final Host hostNodeBReceiver = getNodeReceiverGood();
        final Host hostNodeASender = getNodeSenderGood();

        final PeerId peerId = hostNodeBReceiver.getPeerId();
        Multiaddr hostNodeReceiverPeerId = new Multiaddr(
            STRING_CONNECTION_RECEIVER_PEER + PROTOCOL_P2P + peerId);
        Thread.sleep(1000);

        log.info("THE SENDER ID IS: {}", hostNodeASender.getPeerId());
        StreamPromise<Void> streamPromise = hostNodeASender.newStream(List.of("/channel-a/1.0.0"),
            peerId, hostNodeReceiverPeerId);
        streamPromise.getStream().join();

        Thread.sleep(1000);
    }
}
