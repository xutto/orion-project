package com.mac.orion.application.in;

import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileTransferData;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

public interface FileDownloadOrchestratorAsClientUseCase {

  FileTransferData transferFileProcess(FileTransferData fileTransferData,
      BitSet storedChunks, ConcurrentHashMap<Integer, FileChunk> chunks);

}
