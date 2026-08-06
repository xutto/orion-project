package com.mac.orion.hardcorP2P;

import io.libp2p.core.P2PChannel;
import io.libp2p.core.Stream;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class ProtocolRequest01 implements
    ProtocolBinding<Void> { // todo puede estar inyectado como bean? , puede manipular el hostNode?

//    private final Host hostNode;
    private final MessageHandlerImplNode01 messageHandler;

    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return new ProtocolDescriptor(List.of("/channel-a/1.0.0")); // todo configurable
    }

    @NotNull
    @Override
    public CompletableFuture<? extends Void> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {

        Stream stream = (Stream) p2PChannel;
        stream.pushHandler(messageHandler);

        if (stream.isInitiator()){
            log.info("-SENDER NODE 01 EXECUTE - peticion de informacion de archivo: {} - S: {}", stream, s);
            final String message = "message from node 01"; // todo a estatico o config
            final byte[] concatenatedBytes = message.getBytes(StandardCharsets.UTF_8);
            ByteBuf buffer = Unpooled.wrappedBuffer(concatenatedBytes);
            stream.writeAndFlush(buffer);
        }



//        stream.close();
        return CompletableFuture.completedFuture(null);
    }
}
