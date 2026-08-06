package com.mac.orion.integration.protocol;

import io.libp2p.core.Host;
import io.libp2p.core.P2PChannel;
import io.libp2p.core.multistream.ProtocolBinding;
import io.libp2p.core.multistream.ProtocolDescriptor;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class SegmentPositionProtocol implements ProtocolBinding<Void> {


    private final Host hostNode;

    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public CompletableFuture<? extends Void> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {
        return null;
    }
}
