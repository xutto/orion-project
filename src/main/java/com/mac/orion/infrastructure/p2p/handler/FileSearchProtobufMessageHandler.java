package com.mac.orion.infrastructure.p2p.handler;

import com.mac.orion.application.service.publisher.SearchReceiverPublisherService;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSearchController;
import com.mac.orion.infrastructure.p2p.model.Search;
import com.mac.orion.infrastructure.p2p.receiver.FileSearchReceiver;
import com.mac.orion.infrastructure.p2p.sender.FileSearchSender;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileSearchProtobufMessageHandler extends
    ProtobufProtocolHandler<FileSearchController> {

  public static final int MAX_MESSAGE_SIZE = 1024 * 1024;

  private final SearchReceiverPublisherService searchReceiverPublisherService;
  private final SearchMapper searchMapper;

  public FileSearchProtobufMessageHandler(
      SearchReceiverPublisherService searchReceiverPublisherService, SearchMapper searchMapper) {
    super(Search.SearchInfo.getDefaultInstance(), MAX_MESSAGE_SIZE, MAX_MESSAGE_SIZE);
    this.searchReceiverPublisherService = searchReceiverPublisherService;
    this.searchMapper = searchMapper;
  }


  @NotNull
  @Override
  protected CompletableFuture<FileSearchController> onStartInitiator(@NotNull Stream stream) {
    log.info("onStartInitiator invoked for stream: {}", stream);
    final FileSearchSender fileSearchSender = new FileSearchSender(stream);
    stream.pushHandler(fileSearchSender);

    // add extra time to reorganize protocols on Negotiator.kt
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100); // 50ms debería ser suficiente
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return fileSearchSender;
    });
  }

  @NotNull
  @Override
  protected CompletableFuture<FileSearchController> onStartResponder(@NotNull Stream stream) {
    log.info("onStartResponder invoked for stream: {}", stream);

    FileSearchReceiver fileSearchReceiver = new FileSearchReceiver(
        searchReceiverPublisherService, searchMapper);
    stream.pushHandler(fileSearchReceiver);
    return CompletableFuture.completedFuture(fileSearchReceiver);
  }
}
