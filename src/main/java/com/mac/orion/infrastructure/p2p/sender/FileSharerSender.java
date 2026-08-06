package com.mac.orion.infrastructure.p2p.sender;

import com.mac.orion.infrastructure.p2p.controller.FileSharerController;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharer;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileSharerSender implements ProtocolMessageHandler<FileSharer>,
    FileSharerController {

  @Getter
  private final Stream stream;

  @Override
  public CompletableFuture<Boolean> send(FileSharer message) {
    //todo investigate async option
    log.debug("Sending blocked message: {}", message);

    final byte[] byteArray = message.toByteArray();
    log.debug("byte-array: {}", byteArray);
    stream.writeAndFlush(message);
    stream.closeWrite();
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<PeerInfoDiscovery> sendAsync(FileSharer message) {
    //todo investigate async option
    return null;
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream sender FileSharer closed: {}", stream);
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.warn("FileSharerSender was error, the stream info: {}", stream);
    log.error("Exception received ", cause);
  }


}
