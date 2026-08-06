package com.mac.orion.infrastructure.p2p.handler;

import com.mac.orion.application.in.UpdateFileRoutingTableUseCase;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSharerController;
import com.mac.orion.infrastructure.p2p.model.Manifest.FileSharer;
import com.mac.orion.infrastructure.p2p.receiver.FileSharerReceiver;
import com.mac.orion.infrastructure.p2p.sender.FileSharerSender;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileSharerProtobufMessageHandler extends
    ProtobufProtocolHandler<FileSharerController> {

  public static final int MAX_MESSAGE_SIZE = 1024 * 1024;

  private final FilesMapper filesMapper;
  private final UpdateFileRoutingTableUseCase updateFileRoutingTableUseCase;

  public FileSharerProtobufMessageHandler(FilesMapper filesMapper,
      UpdateFileRoutingTableUseCase updateFileRoutingTableUseCase) {
    super(FileSharer.getDefaultInstance(), MAX_MESSAGE_SIZE, MAX_MESSAGE_SIZE);
    this.filesMapper = filesMapper;
    this.updateFileRoutingTableUseCase = updateFileRoutingTableUseCase;
  }

  @NotNull
  @Override
  protected CompletableFuture<FileSharerController> onStartInitiator(@NotNull Stream stream) {
    log.info("onStartInitiator invoked for stream: {}", stream);

    FileSharerSender fileSharerSender = new FileSharerSender(stream);
    stream.pushHandler(fileSharerSender);

    // add extra time to reorganize protocols on Negotiator.kt
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100); // 50ms debería ser suficiente
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return fileSharerSender;
    });
  }

  @NotNull
  @Override
  protected CompletableFuture<FileSharerController> onStartResponder(@NotNull Stream stream) {

    log.info("onStartResponder invoked for stream: {}", stream);
    FileSharerReceiver fileSharerReceiver = new FileSharerReceiver(filesMapper,
        updateFileRoutingTableUseCase);
    stream.pushHandler(fileSharerReceiver);

    return CompletableFuture.completedFuture(fileSharerReceiver);
  }

}
