package com.mac.orion.infrastructure.p2p.sender;

import com.mac.orion.application.in.FileDownloadOrchestratorAsClientUseCase;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileTransferController;
import com.mac.orion.infrastructure.p2p.model.FileTransfer;
import com.mac.orion.infrastructure.p2p.model.FileTransfer.FileMessage;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileTransferSender implements ProtocolMessageHandler<FileTransfer.FileMessage>,
    FileTransferController {

  private final Stream stream;
  private final FileDownloadOrchestratorAsClientUseCase fileDownloadOrchestratorAsClientService;
  private final FilesMapper filesMapper;
  private final BitSet storedChunks = new BitSet();
  private final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Boolean> send(FileMessage message) {
    stream.writeAndFlush(message);
    return CompletableFuture.completedFuture(true);
  }

  @Override
  public CompletableFuture<FileMessage> sendAsync(FileMessage message) {
    return null;
  }

  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream sender {} closed: {}", this.getClass().getName(), stream);
  }

  @Override
  public void onMessage(@NotNull Stream stream, FileMessage msg) {
    final FileTransferData data = filesMapper.fileTransferToDomain(msg);

    final FileTransferData fileTransferData =
        fileDownloadOrchestratorAsClientService.transferFileProcess(data, storedChunks, chunks);

    final FileMessage fileMessage = filesMapper.domainToFileTransfer(fileTransferData);
    log.debug("[transfer-process] - fileMessage to send: {}", fileMessage);
    stream.writeAndFlush(fileMessage);
  }
}
