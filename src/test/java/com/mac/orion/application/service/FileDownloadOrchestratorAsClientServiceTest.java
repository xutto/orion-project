package com.mac.orion.application.service;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.application.service.agent.GlobalDownloadAgent;
import com.mac.orion.application.service.publisher.FileDownloadPublisherService;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.domain.model.transfer.Sidecar;
import com.mac.orion.support.FileTransferSupport;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static com.mac.orion.domain.dht.OperationsType.RETRIEVE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileDownloadOrchestratorAsClientServiceTest extends BaseUnitTest {

  @Mock
  FileAllocatorService fileAllocatorService;
  @Mock
  FileDownloadPublisherService fileDownloadPublisherService;
  @Mock
  GlobalDownloadAgent globalDownloadAgent;


  @Test
  void givenAvailability_whenTransferProcess_NEXT_WINDOW_thenReturnIndexOK() throws IOException {

    final BitSet storedChunks = new BitSet();
    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();

    final FileTransferData fileTransferDataFromHost = FileTransferSupport.buildFileAvailability();

    Sidecar sidecar = FileTransferSupport.buildSidecar(
        fileTransferDataFromHost.getSize()).build();

    when(fileAllocatorService.loadSidecarBitset(fileTransferDataFromHost.getHash()))
        .thenReturn(sidecar);
    when(fileAllocatorService.tryClaim(fileTransferDataFromHost.getHash(), 0)).thenReturn(true);

    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    assertEquals(MessageType.NEXT_WINDOW, result.getMessageType());
    assertEquals(0, result.getNextWindow().getIndex());

  }

  @Test
  void givenAvailabilityAndUnavailableFragment_whenTransferProcess_thenReturnTransferEnd()
      throws IOException {

    // Given
    final BitSet storedChunks = new BitSet();
    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();
    final FileTransferData fileTransferDataFromHost = FileTransferSupport.buildFileAvailability();
    Sidecar sidecar = FileTransferSupport.buildSidecar(
        fileTransferDataFromHost.getSize()).build();

    when(fileAllocatorService.loadSidecarBitset(fileTransferDataFromHost.getHash()))
        .thenReturn(sidecar);
    when(fileAllocatorService.tryClaim(fileTransferDataFromHost.getHash(), 0)).thenReturn(false);

    // When
    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    // Then
    assertEquals(MessageType.TRANSFER_END, result.getMessageType());
  }

  @Test
  void givenFileChunk_whenTransferProcess_FILE_CHUNK_thenStoreChunk() {

    final byte[] fragmentBytes = new byte[8 * 1024 * 1024];
    int chunkSize = 256 * 1024;
    int chunkIndexProvider = 0;
    int totalChunks = (fragmentBytes.length + chunkSize - 1) / chunkSize; // ceil
    final FileTransferData fileTransferDataFromHost = FileTransferSupport
        .buildFileChunkHost(fragmentBytes, chunkIndexProvider, chunkSize);

    final BitSet storedChunks = new BitSet(totalChunks);
    storedChunks.set(0, totalChunks, false);
    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();

    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    assertEquals(MessageType.FILE_CHUNK, result.getMessageType());
    assertEquals(1, chunks.size());
    assertTrue(storedChunks.get(0));

  }

  @Test
  void givenChunkIsNotNecessary_whenTransferProcess_FILE_CHUNK_thenReturnTransferEndAndRetryWholeFragment() {

    final byte[] fragmentBytes = new byte[8 * 1024 * 1024];
    int chunkSize = 256 * 1024;
    int chunkIndexProvider = 0;
    int totalChunks = (fragmentBytes.length + chunkSize - 1) / chunkSize; // ceil
    final FileTransferData fileTransferDataFromHost = FileTransferSupport
        .buildFileChunkHost(fragmentBytes, chunkIndexProvider, chunkSize);

    final BitSet storedChunks = new BitSet(totalChunks);
    storedChunks.set(0, totalChunks, true);
    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();

    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    assertEquals(MessageType.TRANSFER_END, result.getMessageType());
    verify(fileDownloadPublisherService, timeout(2000).times(1))
        .publish(eq(RETRIEVE), any(File.class));

  }

  @Test
  void givenLastFileChunkNoLastFragment_whenTransferProcess_FILE_CHUNK_thenReturnTransferEnd() {

    // phase: a requested chunk arrives and it is the last one, chunks already exist and the fragment will be formed

    final byte[] fragmentBytes = new byte[8 * 1024 * 1024]; // real size
    int chunkSize = 128 * 1024;
    int chunkSizeStored = 256 * 1024;
    int totalChunks = (fragmentBytes.length + chunkSize - 1) / chunkSize; // ceil
    int chunkIndexProvider = totalChunks - 1;
    final FileTransferData fileTransferDataFromHost = FileTransferSupport
        .buildFileChunkHost(fragmentBytes, chunkIndexProvider, chunkSize);

    final BitSet storedChunks = new BitSet(totalChunks);

    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();
    for (int i = 0; i < totalChunks - 1; i++) {
      final FileChunk fileChunkStored = FileTransferSupport.buildFileChunkHost(fragmentBytes, i,
              chunkSizeStored)
          .getFileChunk();
      storedChunks.set(i, totalChunks - 1);
      chunks.put(i, fileChunkStored);
    }

    final ByteBuffer returnAssembled = ByteBuffer.allocate(
        fileTransferDataFromHost.getFileFragment().getSize());
    when(fileAllocatorService.assembleFragment(chunks,
        fileTransferDataFromHost.getFileFragment())).thenReturn(returnAssembled);
    when(fileAllocatorService.writeFragmentToFile(fileTransferDataFromHost.getFileFragment(),
        returnAssembled, fileTransferDataFromHost.getHash())).thenReturn(
        false); // THIS IS SWITCH TO Final fragment or not

    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    assertEquals(MessageType.TRANSFER_END, result.getMessageType());
    assertEquals(fileTransferDataFromHost.getHash(), result.getHash());

    Awaitility.await()
        .alias("verify is not final fragment FAILED or not PUBLISH file")
        .timeout(Duration.ofSeconds(5))
        .atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
//              verify(fileAllocatorService, timeout(2000))
//                  .deleteClaim(fileTransferDataFromHost.getHash(),
//                      fileTransferDataFromHost.getFileFragment().getIndex());
              verify(fileAllocatorService, timeout(2000)
                  .times(0)).finalizeFile(fileTransferDataFromHost);
              verify(fileDownloadPublisherService, timeout(2000).times(1)).publish(eq(RETRIEVE), any(
                  File.class));
            }
        );


  }

  @Test
  void givenLastFileChunkAndLastFragment_whenTransferProcess_FILE_CHUNK_thenReturnTransferEnd() {

    // phase: a requested chunk arrives and it is the last one, chunks already exist and the fragment will be formed

    final byte[] fragmentBytes = new byte[8 * 1024 * 1024]; // real size
    int chunkSize = 128 * 1024;
    int chunkSizeStored = 256 * 1024;
    int totalChunks = (fragmentBytes.length + chunkSize - 1) / chunkSize; // ceil
    int chunkIndexProvider = totalChunks - 1;
    final FileTransferData fileTransferDataFromHost = FileTransferSupport
        .buildFileChunkHost(fragmentBytes, chunkIndexProvider, chunkSize);

    final BitSet storedChunks = new BitSet(totalChunks);

    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();
    for (int i = 0; i < totalChunks - 1; i++) {
      final FileChunk fileChunkStored = FileTransferSupport.buildFileChunkHost(fragmentBytes, i,
              chunkSizeStored)
          .getFileChunk();
      storedChunks.set(i, totalChunks - 1);
      chunks.put(i, fileChunkStored);
    }

    final ByteBuffer returnAssembled = ByteBuffer.allocate(
        fileTransferDataFromHost.getFileFragment().getSize());
    when(fileAllocatorService.assembleFragment(chunks,
        fileTransferDataFromHost.getFileFragment())).thenReturn(returnAssembled);
    when(fileAllocatorService.writeFragmentToFile(fileTransferDataFromHost.getFileFragment(),
        returnAssembled, fileTransferDataFromHost.getHash())).thenReturn(
        true); // THIS IS SWITCH TO Final fragment or not

    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    assertEquals(MessageType.TRANSFER_END, result.getMessageType());
    assertEquals(fileTransferDataFromHost.getHash(), result.getHash());

    Awaitility.await()
        .alias("verify is not final fragment FAILED or not PUBLISH file")
        .timeout(Duration.ofSeconds(5))
        .atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
              verify(fileAllocatorService, timeout(2000).times(1))
                  .writeFragmentToFile(fileTransferDataFromHost.getFileFragment(),
                      returnAssembled, fileTransferDataFromHost.getHash());
//              verify(fileAllocatorService, timeout(2000)).deleteClaim(fileTransferDataFromHost.getHash(),
//                  fileTransferDataFromHost.getFileFragment().getIndex());
              verify(fileAllocatorService, timeout(2000)
                  .times(1)).finalizeFile(fileTransferDataFromHost);
              verify(fileDownloadPublisherService, timeout(2000).times(0))
                  .publish(eq(RETRIEVE), any(File.class));
            }
        );


  }

  @Test
  void givenLastFileChunkWithInvalidMD5_whenTransferProcess_thenRetryFragmentDownload() {

    // Given
    final byte[] fragmentBytes = new byte[8 * 1024 * 1024]; // real size
    new Random().nextBytes(fragmentBytes);
    int chunkSize = 128 * 1024;
    int chunkSizeStored = 256 * 1024;
    int totalChunks = (fragmentBytes.length + chunkSize - 1) / chunkSize; // ceil
    int chunkIndexProvider = totalChunks - 1;
    final FileTransferData fileTransferDataFromHost = FileTransferSupport
        .buildFileChunkHost(fragmentBytes, chunkIndexProvider, chunkSize);

    final BitSet storedChunks = new BitSet(totalChunks);

    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();
    for (int i = 0; i < totalChunks - 1; i++) {
      final FileChunk fileChunkStored = FileTransferSupport.buildFileChunkHost(fragmentBytes, i,
              chunkSizeStored)
          .getFileChunk();
      storedChunks.set(i, totalChunks - 1);
      chunks.put(i, fileChunkStored);
    }

    final ByteBuffer fakeAssembledBadFragment = ByteBuffer.allocate(8388609); // bad size
    // the fragment assembled is nos correct (md5 check failed) and then: return same index fragment to retry same the window
    when(fileAllocatorService.assembleFragment(chunks,
        fileTransferDataFromHost.getFileFragment())).thenReturn(fakeAssembledBadFragment);
//    doNothing().when(fileAllocatorService).deleteClaim(fileTransferDataFromHost.getHash(),
//        fileTransferDataFromHost.getFileFragment().getIndex());

    // When
    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    // Then
    assertEquals(fileTransferDataFromHost.getHash(), result.getHash());
    assertEquals(MessageType.TRANSFER_END, result.getMessageType());

    Awaitility.await()
//        .alias("verify is not final fragment FAILED or not PUBLISH file")
        .timeout(Duration.ofSeconds(5))
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> {
              verify(fileAllocatorService, timeout(2000).times(0))
                  .writeFragmentToFile(fileTransferDataFromHost.getFileFragment(),
                      fakeAssembledBadFragment, fileTransferDataFromHost.getHash());
//              verify(fileAllocatorService, timeout(2000).times(1)) todo arreglar el delete claim que lo he quitado de su sitio para pruebas
//                  .deleteClaim(fileTransferDataFromHost.getHash(),
//                  fileTransferDataFromHost.getFileFragment().getIndex());

              verify(fileAllocatorService, timeout(2000)
                  .times(0)).finalizeFile(fileTransferDataFromHost);
              verify(fileDownloadPublisherService, timeout(2000).times(1))
                  .publish(eq(RETRIEVE), any(File.class));
            }
        );
  }

  @Test
  void givenLastFileChunk_whenTransferProcess_FILE_CHUNK_And_WriteFragment_thenFinalizeFile() {

    final FileDownloadPublisherService fileDownloadPublisherService = new FileDownloadPublisherService();

    // Given
    final byte[] fragmentBytes = new byte[8 * 1024 * 1024]; // real size
    int chunkSize = 128 * 1024;
    int chunkSizeStored = 256 * 1024;
    int totalChunks = (fragmentBytes.length + chunkSize - 1) / chunkSize; // ceil
    int chunkIndexProvider = totalChunks - 1;
    final FileTransferData fileTransferDataFromHost = FileTransferSupport
        .buildFileChunkHost(fragmentBytes, chunkIndexProvider, chunkSize);

    final BitSet storedChunks = new BitSet(totalChunks);

    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();
    for (int i = 0; i < totalChunks - 1; i++) {
      final FileChunk fileChunkStored = FileTransferSupport.buildFileChunkHost(fragmentBytes, i,
              chunkSizeStored)
          .getFileChunk();
      storedChunks.set(i, totalChunks - 1);
      chunks.put(i, fileChunkStored);
    }

    final ByteBuffer returnAssembled = ByteBuffer.allocate(
        fileTransferDataFromHost.getFileFragment().getSize());
    when(fileAllocatorService.assembleFragment(chunks,
        fileTransferDataFromHost.getFileFragment())).thenReturn(returnAssembled);

    // mock return the fragment is the last fragment
    when(fileAllocatorService.writeFragmentToFile(fileTransferDataFromHost.getFileFragment(),
        returnAssembled, fileTransferDataFromHost.getHash())).thenReturn(true);

    // When
    final FileDownloadOrchestratorAsClientService fileDownloadOrchestratorAsClientService =
        new FileDownloadOrchestratorAsClientService(fileAllocatorService,
            fileDownloadPublisherService, globalDownloadAgent);
    final FileTransferData result = fileDownloadOrchestratorAsClientService.transferFileProcess(
        fileTransferDataFromHost, storedChunks, chunks);

    // Then
    assertEquals(MessageType.TRANSFER_END, result.getMessageType());
    assertEquals(fileTransferDataFromHost.getHash(), result.getHash());
    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> verify(fileAllocatorService).finalizeFile(fileTransferDataFromHost));
  }

}
