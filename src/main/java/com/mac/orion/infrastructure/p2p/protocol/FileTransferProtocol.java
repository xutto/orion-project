package com.mac.orion.infrastructure.p2p.protocol;

import com.mac.orion.application.in.FileDownloadOrchestratorAsClientUseCase;
import com.mac.orion.application.in.FileDownloadOrchestratorAsHostUseCase;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileTransferController;
import com.mac.orion.infrastructure.p2p.handler.FileTransferProtobufMessageHandler;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileTransferProtocol extends StrictProtocolBinding<FileTransferController> {

  public static final String PROTOCOL_CHANNEL_FILE_TRANSFER_1_0_0 = "/file-transfer/1.0.0";
  public static final String FILE_TRANSFER_PROTOCOL_ID = "FileTransferProtocol";

  public FileTransferProtocol(
      FileDownloadOrchestratorAsHostUseCase fileDownloadOrchestratorAsHostService,
      FileDownloadOrchestratorAsClientUseCase fileDownloadOrchestratorAsClientUseCase,
      FilesMapper filesMapper) {
    super(FILE_TRANSFER_PROTOCOL_ID,
        new FileTransferProtobufMessageHandler(fileDownloadOrchestratorAsHostService,
            fileDownloadOrchestratorAsClientUseCase, filesMapper));
  }

  @NotNull
  @Override
  public ProtocolDescriptor getProtocolDescriptor() {
    return new ProtocolDescriptor(
        List.of(PROTOCOL_CHANNEL_FILE_TRANSFER_1_0_0));
  }

}
