package com.mac.orion.hardcorP2P;

import io.libp2p.core.Host;
import io.libp2p.core.P2PChannel;
import io.libp2p.core.Stream;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ProtocolRequest02 implements ProtocolBinding<Void> {

    private final Host hostNode;

    public ProtocolRequest02(Host hostNode) {
        this.hostNode = hostNode;
    }

    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return new ProtocolDescriptor(List.of("/channel-b/1.0.0")); // todo configurable
    }

    @NotNull
    @Override
    public CompletableFuture<? extends Void> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {
        Stream stream = (Stream) p2PChannel;
        stream.pushHandler(new MessageHandlerImplNode02(hostNode));
        if (stream.isInitiator()) {
            log.info("----SENDER NODE 02 EXECUTE: {} - S: {}", hostNode.getPeerId(), s);

            final String message = "message from node 02";
            final byte[] concatenatedBytes = message.getBytes(StandardCharsets.UTF_8);
            ByteBuf buffer = Unpooled.wrappedBuffer(concatenatedBytes);
            stream.writeAndFlush(buffer);
        }

//        stream.close();

        return CompletableFuture.completedFuture(null);
    }
}
