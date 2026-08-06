package com.mac.orion.application.service;

import static com.mac.orion.domain.dht.OperationsType.COMPLETE;
import static com.mac.orion.domain.dht.OperationsType.RETRIEVE;
import static com.mac.orion.domain.dht.OperationsType.SAVE;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.application.in.FileDownloadOrchestratorAsClientUseCase;
import com.mac.orion.application.in.Publisher;
import com.mac.orion.application.service.agent.GlobalDownloadAgent;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileError;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.domain.model.transfer.NextWindow;
import com.mac.orion.domain.model.transfer.Sidecar;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadOrchestratorAsClientService implements
    FileDownloadOrchestratorAsClientUseCase {

  private final FileAllocatorService fileAllocatorService;
  private final Publisher<File> fileDownloadPublisherService;
  private final GlobalDownloadAgent globalDownloadAgent;
  private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  @Override
  public FileTransferData transferFileProcess(FileTransferData fileTransferData,
      BitSet storedChunks, ConcurrentHashMap<Integer, FileChunk> chunks) {

    try {
      return switch (fileTransferData.getMessageType()) {
        case FILE_AVAILABILITY -> getNextFragment(fileTransferData);
        case FILE_CHUNK -> attendFileTransferData(fileTransferData, storedChunks,
            chunks); //todo el caso del fileChunk empieza por el primer chunk = 0 segun lo que llega del host
        default -> fileTransferData;
      };
    } catch (IOException e) {
      log.error("Error getting next fragment", e); // todo controlar el error
      return FileTransferData.builder()
          .fileError(FileError.builder().message(e.getCause().toString()).build()).build();
    }

  }

  private FileTransferData getNextFragment(FileTransferData fileTransferData) throws IOException {
    final BitSet availabilitySet = fileTransferData.getFileAvailability().getAvailability();
    final Sidecar sidecar = fileAllocatorService.loadSidecarBitset(
        fileTransferData.getHash());
    final BitSet localAvailability = sidecar.getData();
    final int totalFragments = sidecar.getTotalFragments();

    int nextFragment = -1;
    for (int i = localAvailability.nextClearBit(0); i < totalFragments;
        i = localAvailability.nextClearBit(i + 1)) {
      final boolean isAvailableFragment = availabilitySet.get(i);

      if (isAvailableFragment) {

        final boolean claimFragment = fileAllocatorService.tryClaim(fileTransferData.getHash(), i);

        if (claimFragment) {
          nextFragment = i;
          break;
        }

      }

    }  //todo if nextfragment -1 means have not fragments available
    if (nextFragment == -1) {
      return FileTransferData.builder()
          .messageType(MessageType.TRANSFER_END)
          .build();
    }
    final NextWindow nextWindow = NextWindow.builder().index(nextFragment).build();
    return FileTransferData.builder()
        .hash(fileTransferData.getHash())
        .messageType(MessageType.NEXT_WINDOW)
        .nextWindow(nextWindow)
        .build();
  }

  private FileTransferData attendFileTransferData(FileTransferData fileTransferData,
      BitSet storedChunks, ConcurrentHashMap<Integer, FileChunk> chunks) throws IOException {

    final FileChunk fileChunk = fileTransferData.getFileChunk();

    if (null != fileTransferData.getFileChunk().getData()) {
      globalDownloadAgent.audit(fileTransferData);
    }

    final int nextChunkMissing = storedChunks.nextClearBit(0);

    // put chunks on the map
    chunks.put(fileChunk.getIndex(), fileChunk);
    storedChunks.set(fileChunk.getIndex());

    // check the chunk is necessary if not, reclaim the file process again through the RETRIEVE option
    if (nextChunkMissing != fileChunk.getIndex()) {
      log.error("Chunk index [{}] is not the next one", nextChunkMissing);
      final File file = File.builder()
          .hash(fileTransferData.getHash())
          .build();
      fileDownloadPublisherService.publish(RETRIEVE, file);
      return finalizeFragmentTransfer(fileTransferData);
    }

    // last chunk
    if (fileChunk.getLast()) {
      executor.execute(
          () -> executeAssembleFragment(fileTransferData, storedChunks, chunks));
      return finalizeFragmentTransfer(fileTransferData);
    }

    // not last chunk
    Integer nextFileChunkIndex = storedChunks.nextClearBit(fileChunk.getIndex() + 1);

    FileChunk nextFileChunk = FileChunk.builder()
        .index(nextFileChunkIndex)
        .last(false)
        .build();

    return FileTransferData.builder()
        .hash(fileTransferData.getHash())
        .messageType(MessageType.FILE_CHUNK)
        .fileChunk(nextFileChunk)
        .build();


  }

  private void executeAssembleFragment(FileTransferData fileTransferData, BitSet storedChunks,
      ConcurrentHashMap<Integer, FileChunk> chunks) {

    // 0) define a public file
    final File filePublic = File.builder()
        .hash(fileTransferData.getHash())
        .build();

    try {
      // 1) Ensamblar el fragmento a partir de los chunks
      final ByteBuffer assembledFragment = fileAllocatorService.assembleFragment(
          chunks, fileTransferData.getFileFragment());

      // 2) Calcular MD5 y compararlo con el hash esperado
      final boolean md5IsExpected = checkMD5IsExpected(fileTransferData, storedChunks, chunks,
          assembledFragment);

      if (md5IsExpected) {

        // 4) MD5 OK: escribir el fragmento en el fichero, devuelve si es el último fragmento
        final boolean finalFragment = fileAllocatorService.writeFragmentToFile(
            fileTransferData.getFileFragment(), assembledFragment, fileTransferData.getHash());

        if (finalFragment) {
          log.info("the final fragment was written, try to finalize file: {}",
              fileTransferData.getHash());
          try {
            fileAllocatorService.finalizeFile(fileTransferData);
            fileDownloadPublisherService.publish(COMPLETE, filePublic);
          } catch (IOException e) {
            log.error("Error finalizing file", e);
            throw new RuntimeException(e);
          }
        } else {
          fileDownloadPublisherService.publish(RETRIEVE, filePublic);
        }
      } else {
        fileDownloadPublisherService.publish(RETRIEVE, filePublic);
      }
    } finally {
      fileDownloadPublisherService.publish(SAVE, filePublic);
    }

    // delete claim lock file
//    fileAllocatorService.deleteClaim(fileTransferData.getHash(),
//        fileTransferData.getFileFragment().getIndex());
  }

  private boolean checkMD5IsExpected(FileTransferData fileTransferData, BitSet storedChunks,
      ConcurrentHashMap<Integer, FileChunk> chunks, ByteBuffer assembledFragment) {
    final String computedMd5 = FileProcessUtils.checksumMD5FromByteBuffer(assembledFragment,
        fileTransferData.getFileFragment().getSize());
    final String expectedMd5 = fileTransferData.getFileFragment().getHash().getValue();
    final boolean md5IsOK = computedMd5.equalsIgnoreCase(expectedMd5);

    if (!md5IsOK) {
      log.warn("MD5 KO: fragment index [{}]", fileTransferData.getFileFragment().getIndex());
    }
    return md5IsOK;
  }

  private FileTransferData finalizeFragmentTransfer(FileTransferData fileTransferData) {
    return FileTransferData.builder()
        .messageType(MessageType.TRANSFER_END)
        .hash(fileTransferData.getHash())
        .build();

  }
}

