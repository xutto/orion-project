package com.mac.orion.hardcorP2P;

import io.libp2p.core.Host;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MessageHandlerImplNode01 implements ProtocolMessageHandler<ByteBuf> {

    private final Host hostNode;

    public MessageHandlerImplNode01(Host hostNode) {
        this.hostNode = hostNode;
    }

    @Override
    public void onMessage(@NotNull Stream stream, ByteBuf msg) {

        log.info("Message received NODE_01: {}", hostNode.getPeerId());
        log.info("Message received NODE_01: FROM {}", stream.remotePeerId());
        hostNode.newStream(List.of("/channel-b/1.0.0"), stream.getConnection());


//        stream.close();
//        StreamPromise<Void> streamPromise = hostNode.newStream(List.of("/channel-b/1.0.0"), stream.getConnection().remoteAddress().getPeerId());
//        streamPromise.getStream().join();
    }
}
