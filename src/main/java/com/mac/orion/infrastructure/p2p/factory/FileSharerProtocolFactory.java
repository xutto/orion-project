package com.mac.orion.infrastructure.p2p.factory;

import com.mac.orion.application.in.UpdateFileRoutingTableUseCase;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSharerController;
import com.mac.orion.infrastructure.p2p.protocol.FileSharerProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileSharerProtocolFactory implements ProtocolFactory<FileSharerController>{

  private final FilesMapper filesMapper;
  private final UpdateFileRoutingTableUseCase updateFileRoutingTableUseCase;

  @Override
  public FileSharerProtocol getProtocol() {
    return new FileSharerProtocol(filesMapper, updateFileRoutingTableUseCase);
  }
}
