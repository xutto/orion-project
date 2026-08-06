package com.mac.orion.infrastructure.p2p.receiver;


import com.mac.orion.application.in.UpdateFileRoutingTableUseCase;
import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSharerController;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharer;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharerRequest;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileSharerReceiver implements ProtocolMessageHandler<FileSharer>,
    FileSharerController {

  private final FilesMapper filesMapper;
  private final UpdateFileRoutingTableUseCase updateFileRoutingTableUseCase;

  @Override
  public CompletableFuture<Boolean> send(FileSharer message) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public CompletableFuture<PeerInfoDiscovery> sendAsync(FileSharer message) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public void onMessage(@NotNull Stream stream, FileSharer msg) {

    final FileSharerRequest request = msg.getRequest();

    final Set<File> files = request.getFilesManifestList().stream()
        .map(filesMapper::fileManifestToDomain)
        .collect(Collectors.toSet());

    log.info("Received files: {}", files);
    updateFileRoutingTableUseCase.updateFileRoutingTable(files);

    stream.closeWrite();
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream receiver FileSharer closed: {}", stream);
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }
}
