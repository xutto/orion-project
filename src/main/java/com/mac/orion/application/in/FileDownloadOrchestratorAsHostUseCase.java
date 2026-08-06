package com.mac.orion.application.in;

import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import java.util.concurrent.ConcurrentHashMap;

public interface FileDownloadOrchestratorAsHostUseCase {


  FileTransferData performFileTransfer(FileTransferData fileTransferData,
      ConcurrentHashMap<Hash, FileFragment> fragmentData);
}
