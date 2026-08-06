package com.mac.orion.hardcorP2P;

import io.libp2p.core.P2PChannel;
import io.libp2p.core.Stream;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.multistream.ProtocolDescriptor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class MessageProtocolNode02 implements ProtocolBinding<Void> {
    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return new ProtocolDescriptor(List.of("/channel-a/1.0.0")); // todo configurable
    }

    @NotNull
    @Override
    public CompletableFuture<? extends Void> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {

        Stream stream = (Stream) p2PChannel;
        log.info("PROTOCOL OF NODE 02");

//        stream.pushHandler(null);


        return CompletableFuture.completedFuture(null);
    }
}
