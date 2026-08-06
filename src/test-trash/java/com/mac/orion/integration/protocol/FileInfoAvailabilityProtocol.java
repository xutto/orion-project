package com.mac.orion.integration.protocol;

import com.mac.orion.integration.dto.TransferDto;
import com.mac.orion.integration.handler.FileInfoAvailabilityHandler;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Slf4j
public class FileInfoAvailabilityProtocol implements ProtocolBinding<Void> {

//    private final Host hostNode;
    private final FileInfoAvailabilityHandler fileInfoAvailabilityHandler01;

//    @Getter
    @Setter
    private TransferDto fileInfoAvailabilityDTO;


    @NotNull
    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
        return new ProtocolDescriptor(List.of("/file-info-availability/1.0.0"));
    }

    @NotNull
    @Override
    public CompletableFuture<? extends Void> initChannel(@NotNull P2PChannel p2PChannel, @NotNull String s) {

        Stream stream = (Stream) p2PChannel;
        stream.pushHandler(fileInfoAvailabilityHandler01);

        if (stream.isInitiator()){
            log.info("SENDER NODE 01 EXECUTE - peticion de informacion de archivo: {} - S: {}", stream, s);
            final String message = "message from node 01"; // todo a estatico o config
            final byte[] concatenatedBytes = message.getBytes(StandardCharsets.UTF_8);
            ByteBuf buffer = Unpooled.wrappedBuffer(concatenatedBytes);
            stream.writeAndFlush(buffer);
        }

        return CompletableFuture.completedFuture(null);
    }
}
