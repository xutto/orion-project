package com.mac.orion.integration.handler;

import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class SegmentsAvailableHandler implements ProtocolMessageHandler<ByteBuf> {

    @Override
    public void onMessage(@NotNull Stream stream, ByteBuf msg) {
        ProtocolMessageHandler.super.onMessage(stream, msg);
    }
}
