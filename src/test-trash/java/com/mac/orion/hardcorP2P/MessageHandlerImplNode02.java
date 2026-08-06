package com.mac.orion.hardcorP2P;

import io.libp2p.core.Host;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MessageHandlerImplNode02 implements ProtocolMessageHandler<ByteBuf> {

    private final Host hostNode;

    public MessageHandlerImplNode02(Host hostNode) {
        this.hostNode = hostNode;
    }

    @Override
    public void onMessage(@NotNull Stream stream, ByteBuf msg) {

        log.info("Message received NODE_02: {}", hostNode.getPeerId());
        stream.close();
    }
}
