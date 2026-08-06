package com.mac.orion.application.service;

import static com.mac.orion.application.service.FileAllocatorService.CHUNK_SIZE;
import static com.mac.orion.application.service.FileAllocatorService.FRAGMENT_SIZE;
import static com.mac.orion.application.service.FileAllocatorServiceTest.HASH_DEFAULT;
import static com.mac.orion.application.service.FileAllocatorServiceTest.SIZE_3GB;
import static com.mac.orion.application.service.FileAllocatorServiceTest.STORED_FOLDER;
import static com.mac.orion.application.service.FileAllocatorServiceTest.TEMP_FOLDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.application.out.FilesPort;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Status;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.domain.model.transfer.Sidecar;
import com.mac.orion.support.FileTransferSupport;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class FileDownloadOrchestratorAsHostServiceTest extends BaseUnitTest {

  @InjectMocks
  FileDownloadOrchestratorAsHostService fileDownloadOrchestratorAsHostService;

  @Mock
  private FilesPort filesPort;
  @Mock
  private FileAllocatorService fileAllocatorService;


  @Test
  void givenExistingFile_whenPerformFileTransferWithSTORED_thenReturnsFileAvailability()
      throws IOException {

    // Given

    // Existing a full file
    final ConcurrentHashMap<Hash, FileFragment> fragmentData = new ConcurrentHashMap<>();
    final FileTransferData fileTransferData = FileTransferSupport.buildFileGet();
    final File file = FileTransferSupport.buildFile(
        STORED_FOLDER + "/" + "fake_file_01.txt", SIZE_3GB);
    Files.createDirectories(Path.of(STORED_FOLDER));
    final Path filePath = Path.of(file.getPath());
    try (FileChannel ch = FileChannel.open(filePath,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      ch.truncate(file.getSize());
      ch.force(true);
    }
    assertTrue(Files.exists(filePath));

    when(filesPort.findByHash(fileTransferData.getHash().getValue())).thenReturn(file);
    final int fragments = calculatedFragments(file.getSize());

    BitSet bitset = new BitSet(fragments);
    bitset.set(0, fragments);

    when(fileAllocatorService.getFragmetsBySize(file.getSize())).thenReturn(fragments);
    when(fileAllocatorService.createFullBitSet(fragments)).thenReturn(bitset);

    // When
    final FileTransferData result = fileDownloadOrchestratorAsHostService.performFileTransfer(
        fileTransferData, fragmentData);

    // Then
    assertEquals(MessageType.FILE_AVAILABILITY, result.getMessageType());
    assertEquals(file.getSize(), result.getSize());
    assertEquals(bitset, result.getFileAvailability().getAvailability());
    assertNotNull(result.getHash());
  }

  @Test
  void givenPartialFile_whenPerformFileTransfer_thenReturnsPartialAvailability()
      throws IOException {

    // Given
    int fragmentCount = 1024;
    final BitSet bitSet = new BitSet(fragmentCount);
    bitSet.set(5, 512); // random completed fragments
    final Sidecar sidecarFake = FileTransferSupport.buildSidecar(fragmentCount).data(bitSet)
        .build();

    // Existing a partial file
    final ConcurrentHashMap<Hash, FileFragment> fragmentData = new ConcurrentHashMap<>();
    final FileTransferData fileTransferData = FileTransferSupport.buildFileGet();
    final File fileRawDontUse = FileTransferSupport.buildFile(
        TEMP_FOLDER + "/" + "2fd4e1c67a2d28fced849ee1bb76e739", SIZE_3GB);
    final File fileMod = fileRawDontUse.toBuilder().status(Status.DOWNLOADING).build();
    Files.createDirectories(Path.of(TEMP_FOLDER));
    final Path filePath = Path.of(fileMod.getPath());
    try (FileChannel ch = FileChannel.open(filePath,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      ch.truncate(fileMod.getSize());
      ch.force(true);
    }

    when(filesPort.findByHash(fileTransferData.getHash().getValue())).thenReturn(fileMod);
    when(fileAllocatorService.loadSidecarBitset(fileMod.getHash())).thenReturn(sidecarFake);


    // When
    final FileTransferData result = fileDownloadOrchestratorAsHostService.performFileTransfer(
        fileTransferData, fragmentData);

    // Then
    assertEquals(MessageType.FILE_AVAILABILITY, result.getMessageType());
    assertEquals(fileRawDontUse.getSize(), result.getSize());
    assertEquals(bitSet, result.getFileAvailability().getAvailability());

  }

  @Test
  void givenErrorLoadingSidecar_whenPerformFileTransfer_thenThrowsIOException() throws IOException {

    // Given
    // Not existing a partial file
    final ConcurrentHashMap<Hash, FileFragment> fragmentData = new ConcurrentHashMap<>();
    final FileTransferData fileTransferData = FileTransferSupport.buildFileGet();
    final File fileRawDontUse = FileTransferSupport.buildFile(
        TEMP_FOLDER + "/" + "2fd4e1c67a2d28fced849ee1bb76e739", SIZE_3GB);
    final File fileMod = fileRawDontUse.toBuilder().status(Status.DOWNLOADING).build();
    Files.createDirectories(Path.of(TEMP_FOLDER));
    final Path filePath = Path.of(fileMod.getPath());
    try (FileChannel ch = FileChannel.open(filePath,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      ch.truncate(fileMod.getSize());
      ch.force(true);
    }

    when(filesPort.findByHash(fileTransferData.getHash().getValue())).thenReturn(fileMod);
    when(fileAllocatorService.loadSidecarBitset(fileMod.getHash())).thenThrow(
        new IOException("Error loading sidecar bitset"));


    assertThrows(RuntimeException.class, () -> fileDownloadOrchestratorAsHostService.performFileTransfer(
        fileTransferData, fragmentData), "Error loading sidecar bitset");

  }

  @Test
  void givenNextWindowRequest_whenPerformFileTransfer_thenReturnsFileChunk() {

    // Given
    final FileTransferData fileTransferData = FileTransferSupport.buildNextWindow(7);
    final ConcurrentHashMap<Hash, FileFragment> fragmentDataConcurrentMap = new ConcurrentHashMap<>();
    byte[] fragmentFake = new byte[FRAGMENT_SIZE];
    new Random().nextBytes(fragmentFake);
    byte[] chunkFake = new byte[CHUNK_SIZE];
    new Random().nextBytes(chunkFake);

    when(fileAllocatorService.getFileFragment(fileTransferData.getHash(),
        fileTransferData.getNextWindow().getIndex())).thenReturn(fragmentFake);
    when(fileAllocatorService.getChunkByFragment(fragmentFake,
        0)).thenReturn(chunkFake);

    // When
    final FileTransferData result = fileDownloadOrchestratorAsHostService.performFileTransfer(
        fileTransferData, fragmentDataConcurrentMap);

    // Then
    assertEquals(MessageType.FILE_CHUNK, result.getMessageType());
    assertEquals(fileTransferData.getHash(), result.getHash());
    assertEquals(fileTransferData.getNextWindow().getIndex(), result.getFileFragment().getIndex());
    assertEquals(chunkFake.length, result.getFileChunk().getData().length);
    assertEquals(0, result.getFileChunk().getIndex());
    assertEquals(1, fragmentDataConcurrentMap.size());

  }

  @Test
  void givenExistingFragment_whenPerformFileTransfer_thenReturnsFileChunk() {

    // Given
    byte[] fragmentFakeData = new byte[FRAGMENT_SIZE];
    new Random().nextBytes(fragmentFakeData);
    byte[] chunkFake = new byte[CHUNK_SIZE];
    new Random().nextBytes(chunkFake);
    final ConcurrentHashMap<Hash, FileFragment> fragmentDataConcurrentMap = new ConcurrentHashMap<>();
    final FileFragment fakeFragment = FileTransferSupport.buildFileFragment(fragmentFakeData, 7,
        HASH_DEFAULT);
    fragmentDataConcurrentMap.put(fakeFragment.getHash(), fakeFragment);
    final FileTransferData fileTransferData = FileTransferSupport.buildFileChunkClient(7, 1);

    when(fileAllocatorService.getChunkByFragment(fragmentFakeData,
        1)).thenReturn(chunkFake);

    // When
    final FileTransferData result = fileDownloadOrchestratorAsHostService.performFileTransfer(
        fileTransferData, fragmentDataConcurrentMap);

    // Then
    assertEquals(MessageType.FILE_CHUNK, result.getMessageType());
    assertEquals(fileTransferData.getHash(), result.getHash());
    assertEquals(fileTransferData.getNextWindow().getIndex(), result.getFileFragment().getIndex());
    assertEquals(chunkFake.length, result.getFileChunk().getData().length);
    assertEquals(1, result.getFileChunk().getIndex());
    assertEquals(1, fragmentDataConcurrentMap.size());

  }

  private int calculatedFragments(Long size) {
    return (int) ((size + FRAGMENT_SIZE - 1L) / FRAGMENT_SIZE);
  }
}
