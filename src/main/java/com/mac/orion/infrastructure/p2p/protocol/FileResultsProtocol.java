package com.mac.orion.infrastructure.p2p.protocol;

import com.mac.orion.application.in.SearchFilesResultUseCase;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileResultsController;
import com.mac.orion.infrastructure.p2p.handler.FileResultsProtobufMessageHandler;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileResultsProtocol extends StrictProtocolBinding<FileResultsController> {

  public static final String PROTOCOL_CHANNEL_FILES_RESULT_1_0_0 = "/files-result/1.0.0";
  public static final String FILE_RESULT_PROTOCOL_ID = "FileResultsProtocol";

  public FileResultsProtocol(SearchFilesResultUseCase searchFilesResultUseCase,
      SearchMapper searchMapper) {
    super(FILE_RESULT_PROTOCOL_ID,
        new FileResultsProtobufMessageHandler(searchFilesResultUseCase, searchMapper));
  }

  @NotNull
  @Override
  public ProtocolDescriptor getProtocolDescriptor() {
    return new ProtocolDescriptor(
        List.of(PROTOCOL_CHANNEL_FILES_RESULT_1_0_0));
  }

}


