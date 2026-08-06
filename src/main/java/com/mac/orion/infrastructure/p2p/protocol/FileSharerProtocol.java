package com.mac.orion.infrastructure.p2p.protocol;

import com.mac.orion.application.in.UpdateFileRoutingTableUseCase;
import com.mac.orion.infrastructure.mapper.FilesMapper;
import com.mac.orion.infrastructure.p2p.controller.FileSharerController;
import com.mac.orion.infrastructure.p2p.handler.FileSharerProtobufMessageHandler;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileSharerProtocol extends StrictProtocolBinding<FileSharerController> {

  public static final String PROTOCOL_CHANNEL_FILES_SHARER_1_0_0 = "/files-sharer/1.0.0";
  public static final String FILE_SHARER_PROTOCOL_ID = "FileSharerProtocol";

  public FileSharerProtocol(FilesMapper filesMapper,
      UpdateFileRoutingTableUseCase updateFileRoutingTableUseCase) {
    super(FILE_SHARER_PROTOCOL_ID,
        new FileSharerProtobufMessageHandler(filesMapper, updateFileRoutingTableUseCase));
  }

  @NotNull
  @Override
  public ProtocolDescriptor getProtocolDescriptor() {
    return new ProtocolDescriptor(
        List.of(PROTOCOL_CHANNEL_FILES_SHARER_1_0_0));
  }
}
