package com.mac.orion.infrastructure.p2p.handler;

import com.mac.orion.application.in.SearchFilesResultUseCase;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileResultsController;
import com.mac.orion.infrastructure.p2p.model.SearchResult;
import com.mac.orion.infrastructure.p2p.receiver.FileResultsReceiver;
import com.mac.orion.infrastructure.p2p.sender.FileResultsSender;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileResultsProtobufMessageHandler extends
    ProtobufProtocolHandler<FileResultsController> {

  public static final int MAX_MESSAGE_SIZE = 1024 * 1024;
  private final SearchFilesResultUseCase searchFilesResultUseCase;
  private final SearchMapper searchMapper;


  public FileResultsProtobufMessageHandler(SearchFilesResultUseCase searchFilesResultUseCase,
      SearchMapper searchMapper) {
    super(SearchResult.SearchInfoResult.getDefaultInstance(), MAX_MESSAGE_SIZE, MAX_MESSAGE_SIZE);
    this.searchFilesResultUseCase = searchFilesResultUseCase;
    this.searchMapper = searchMapper;
  }

  @NotNull
  @Override
  protected CompletableFuture<FileResultsController> onStartInitiator(@NotNull Stream stream) {
    final FileResultsSender fileResultsSender = new FileResultsSender(stream);
    stream.pushHandler(fileResultsSender);

    // todo [hack] add extra time to reorganize protocols on Negotiator.kt
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100); // 50ms debería ser suficiente
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return fileResultsSender;
    });
  }

  @NotNull
  @Override
  protected CompletableFuture<FileResultsController> onStartResponder(@NotNull Stream stream) {

    FileResultsReceiver fileSearchReceiverHandler = new FileResultsReceiver(
        searchFilesResultUseCase, searchMapper);
    stream.pushHandler(fileSearchReceiverHandler);
    return CompletableFuture.completedFuture(fileSearchReceiverHandler);

  }
}
