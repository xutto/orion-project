package com.mac.orion.hardcorP2P;

import io.libp2p.core.Host;
import io.libp2p.core.P2PChannel;
import io.libp2p.core.Stream;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.multistream.ProtocolDescriptor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MessageProtocolNode01 implements ProtocolBinding<Void> {

    private final Host hostNode;

    public MessageProtocolNode01(Host hostNode) {
        this.hostNode = hostNode;
    }

    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return new ProtocolDescriptor(List.of("/channel-b/1.0.0")); // todo URGENTE MUCHO CUIDADO SOLISIONA CON EL PROTOCOLO DEL SENDER
    }

    @NotNull
    @Override
    public CompletableFuture<? extends Void> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {

        Stream stream = (Stream) p2PChannel;
        log.info("PROTOCOL OF NODE 01");

        stream.pushHandler(new MessageHandlerImplNode01(hostNode));




        return CompletableFuture.completedFuture(null);
    }
}
