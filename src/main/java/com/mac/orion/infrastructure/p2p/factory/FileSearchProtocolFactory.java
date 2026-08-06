package com.mac.orion.infrastructure.p2p.factory;

import com.mac.orion.application.service.publisher.SearchReceiverPublisherService;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSearchController;
import com.mac.orion.infrastructure.p2p.protocol.FileSearchProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class FileSearchProtocolFactory implements ProtocolFactory<FileSearchController>{

  private final SearchReceiverPublisherService searchReceiverPublisherService;
  private final SearchMapper searchMapper;

  @Override
  public FileSearchProtocol getProtocol() {
    return new FileSearchProtocol(searchReceiverPublisherService, searchMapper);
  }
}
