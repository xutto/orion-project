package com.mac.orion.infrastructure.p2p.protocol;

import com.mac.orion.application.service.publisher.SearchReceiverPublisherService;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSearchController;
import com.mac.orion.infrastructure.p2p.handler.FileSearchProtobufMessageHandler;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileSearchProtocol extends StrictProtocolBinding<FileSearchController> {

  public static final String PROTOCOL_CHANNEL_FILES_SEARCHER_1_0_0 = "/files-searcher/1.0.0";
  public static final String FILE_SEARCHER_PROTOCOL_ID = "FileSearchProtocol";

  public FileSearchProtocol(SearchReceiverPublisherService searchReceiverPublisherService,
      SearchMapper searchMapper) {
    super(FILE_SEARCHER_PROTOCOL_ID,
        new FileSearchProtobufMessageHandler(searchReceiverPublisherService, searchMapper));
  }

  @NotNull
  @Override
  public ProtocolDescriptor getProtocolDescriptor() {
    return new ProtocolDescriptor(
        List.of(PROTOCOL_CHANNEL_FILES_SEARCHER_1_0_0));
  }
}
