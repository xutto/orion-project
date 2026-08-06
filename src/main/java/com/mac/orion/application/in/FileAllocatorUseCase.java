package com.mac.orion.application.in;

import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.Sidecar;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

public interface FileAllocatorUseCase {

  File allocateFilePhysical(File file);

  byte[] getFileFragment(Hash hash, Integer index);

  boolean allocateFileLogical(File file);

  boolean allocateSidecar(File file) throws IOException ;

  Sidecar loadSidecarBitsetUnsafe(Hash hash) throws IOException;

  Sidecar loadSidecarBitset(Hash hash) throws IOException;

  void updateSidecar(Sidecar sidecar, Hash fileHash) throws IOException;

  boolean tryClaim(Hash hash, int pieceIndex);

  void deleteClaim(Hash hash);

  BitSet createClearBitSet(int size);

  BitSet createFullBitSet(int fragments);

  int getFragmetsBySize(Long size);

  int getChunksBySize(int size);

  byte[] getChunkByFragment(byte[] fragment, int index);

  ByteBuffer assembleFragment(ConcurrentHashMap<Integer, FileChunk> chunks,
      FileFragment fileFragmentTemplate);

  /**
   *
   * @param fileFragmentTemplate
   * @param fragmentBuf
   * @param fileHash
   * @return
   */
  boolean writeFragmentToFile(FileFragment fileFragmentTemplate, ByteBuffer fragmentBuf,
      Hash fileHash);


  void finalizeFile(FileTransferData fileTransferData) throws IOException;
}
