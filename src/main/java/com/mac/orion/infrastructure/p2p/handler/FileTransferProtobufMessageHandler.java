package com.mac.orion.infrastructure.p2p.handler;

import com.mac.orion.application.in.FileDownloadOrchestratorAsClientUseCase;
import com.mac.orion.application.in.FileDownloadOrchestratorAsHostUseCase;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileTransferController;
import com.mac.orion.infrastructure.p2p.model.FileTransfer;
import com.mac.orion.infrastructure.p2p.receiver.FileTransferReceiver;
import com.mac.orion.infrastructure.p2p.sender.FileTransferSender;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class FileTransferProtobufMessageHandler extends
    ProtobufProtocolHandler<FileTransferController> {

  public static final int CUSTOM_TRAFFIC_LIMIT =  12 * 1024 * 1024;
  private final FileDownloadOrchestratorAsHostUseCase fileDownloadOrchestratorAsHostService;
  private final FileDownloadOrchestratorAsClientUseCase fileDownloadOrchestratorAsClientUseCase;
  private final FilesMapper filesMapper;

  public FileTransferProtobufMessageHandler(
      FileDownloadOrchestratorAsHostUseCase fileDownloadOrchestratorAsHostService,
      FileDownloadOrchestratorAsClientUseCase fileDownloadOrchestratorAsClientUseCase,
      FilesMapper filesMapper) {
    super(FileTransfer.FileMessage.getDefaultInstance(), /*initiator*/ CUSTOM_TRAFFIC_LIMIT, /*responder*/ CUSTOM_TRAFFIC_LIMIT);
    this.fileDownloadOrchestratorAsHostService = fileDownloadOrchestratorAsHostService;
    this.fileDownloadOrchestratorAsClientUseCase = fileDownloadOrchestratorAsClientUseCase;
    this.filesMapper = filesMapper;
  }

  @NotNull
  @Override
  protected CompletableFuture<FileTransferController> onStartInitiator(@NotNull Stream stream) {
    FileTransferSender fileTransferSender = new FileTransferSender(stream,
        fileDownloadOrchestratorAsClientUseCase, filesMapper);
    stream.pushHandler(fileTransferSender);

    // todo [hack] add extra time to reorganize protocols on Negotiator.kt
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100); // 50ms debería ser suficiente
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return fileTransferSender;
    });

  }

  @NotNull
  @Override
  protected CompletableFuture<FileTransferController> onStartResponder(@NotNull Stream stream) {
    final FileTransferReceiver fileTransferReceiver = new FileTransferReceiver(
        fileDownloadOrchestratorAsHostService, filesMapper);
    stream.pushHandler(fileTransferReceiver);
    return CompletableFuture.completedFuture(fileTransferReceiver);
  }

}
