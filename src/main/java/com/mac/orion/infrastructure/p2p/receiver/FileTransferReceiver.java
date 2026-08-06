package com.mac.orion.infrastructure.p2p.receiver;

import com.mac.orion.application.in.FileDownloadOrchestratorAsHostUseCase;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileTransferController;
import com.mac.orion.infrastructure.p2p.model.FileTransfer;
import com.mac.orion.infrastructure.p2p.model.FileTransfer.FileMessage;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileTransferReceiver implements ProtocolMessageHandler<FileTransfer.FileMessage>,
    FileTransferController {

  private final FileDownloadOrchestratorAsHostUseCase fileDownloadOrchestratorAsHostService;
  private final FilesMapper filesMapper;
  private final ConcurrentHashMap<Hash, FileFragment> fragmentData = new ConcurrentHashMap<>();


  @Override
  public void onMessage(@NotNull Stream stream, FileMessage msg) {

    final FileTransferData fileTransferData = filesMapper.fileTransferToDomain(msg);
    log.info("[transfer-process] - fileTransferData received and mapped: {}", fileTransferData);
    FileTransferData resultExchange = fileDownloadOrchestratorAsHostService.performFileTransfer(
        fileTransferData, fragmentData);

    if(resultExchange.getMessageType().equals(MessageType.TRANSFER_END)){
      stream.closeWrite();
      return;
    }
    log.info("[transfer-process] - resultExchange processed: {}", resultExchange);
    FileMessage fileMessage = filesMapper.domainToFileTransfer(resultExchange);
    log.info("[transfer-process] - fileMessage mapped: {}", fileMessage);
    stream.writeAndFlush(fileMessage);

  }

  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream sender {} closed: {}", this.getClass().getName(), stream);
  }

}

//  @Override
//  public CompletableFuture<Boolean> send(FileMessage fileMessage) {
//    throw new IllegalStateException("Responder only!");
//  } todo probar si lo del default en la interfaz funciona
//
//  @Override
//  public CompletableFuture<FileMessage> sendAsync(FileMessage fileMessage) {
//
//  }
