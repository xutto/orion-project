package com.mac.orion.infrastructure.p2p.factory;

import com.mac.orion.application.in.SearchFilesResultUseCase;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileResultsController;
import com.mac.orion.infrastructure.p2p.protocol.FileResultsProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileResultsProtocolFactory implements ProtocolFactory<FileResultsController> {

  private final SearchFilesResultUseCase searchFilesResultUseCase;
  private final SearchMapper searchMapper;

  @Override
  public  FileResultsProtocol getProtocol() {
    return new FileResultsProtocol(searchFilesResultUseCase, searchMapper);
  }
}
