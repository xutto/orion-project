package com.mac.orion.infrastructure.p2p.factory;

import com.mac.orion.application.in.FileDownloadOrchestratorAsClientUseCase;
import com.mac.orion.application.in.FileDownloadOrchestratorAsHostUseCase;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileTransferController;
import com.mac.orion.infrastructure.p2p.protocol.FileTransferProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileTransferProtocolFactory implements ProtocolFactory<FileTransferController> {

  private final FileDownloadOrchestratorAsHostUseCase fileDownloadOrchestratorAsHostService;
  private final FileDownloadOrchestratorAsClientUseCase fileDownloadOrchestratorAsClientUseCase;
  private final FilesMapper filesMapper;


  @Override
  public FileTransferProtocol getProtocol() {
    return new FileTransferProtocol(fileDownloadOrchestratorAsHostService,
        fileDownloadOrchestratorAsClientUseCase, filesMapper);
  }
}
