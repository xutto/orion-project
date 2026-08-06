package com.mac.orion.application.service;

import static com.mac.orion.application.service.FileAllocatorService.CHUNK_SIZE;
import static com.mac.orion.application.service.FileAllocatorService.EXTENSION_LOCKS;
import static com.mac.orion.application.service.FileAllocatorService.EXTENSION_SIDECAR_XUT;
import static com.mac.orion.application.service.FileAllocatorService.FRAGMENT_SIZE;
import static com.mac.orion.application.service.FileAllocatorService.SIDECAR_HEADER_FRAGMENTS;
import static com.mac.orion.application.service.FileAllocatorService.SIDECAR_HEADER_PAYLOAD;
import static com.mac.orion.application.service.FileAllocatorService.SIDECAR_HEADER_VERSION;
import static com.mac.orion.domain.model.Status.DOWNLOADING;
import static com.mac.orion.domain.model.Status.STORED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.application.out.FilesPort;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.settings.Directories;
import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.domain.model.transfer.Sidecar;
import com.mac.orion.support.FileTransferSupport;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = {"classpath:application-test.yml"})
@Slf4j
class FileAllocatorServiceTest extends BaseUnitTest {

  public static final String TEMP_FOLDER = "target/test-tmp/files/tmp";
  public static final String STORED_FOLDER = "target/test-tmp/files/stored";
  public static final String TEMP_FORBIDDEN_FOLDER = "target/test-tmp/files/forbidden";
  public static final String DOWNLOAD_FOLDER = "target/test-tmp/files/incoming";
  public static final long SIZE_3GB = 1024L * 1024L * 1024 * 3;
  public static final int SIZE_1GB = 1024 * 1024 * 1024;
  public static final int SIZE_8MB = 8 * 1024 * 1024;
  public static final int SIZE_128MB = 128 * 1024 * 1024;
  public static final String FIELD_TEMP_FOLDER = "tempFolder";
  public static final String FIELD_DOWNLOAD_FOLDER = "downloadFolder";
  public static final String HASH_DEFAULT = "2fd4e1c67a2d28fced849ee1bb76e739";
  private Path allocatedFilePath;
  private Path allocatedSidecarPath;
  private Path allocatedClaim;

  @InjectMocks
  FileAllocatorService fileAllocatorService;

  @Mock
  private FilesPort filesPort;
  @Mock
  private Settings settings;

  @AfterEach
  public void cleanup() throws IOException {
    if (allocatedFilePath != null && Files.exists(allocatedFilePath)) {
      Files.delete(allocatedFilePath);
    }
    if (allocatedSidecarPath != null && Files.exists(allocatedSidecarPath)) {
      Files.delete(allocatedSidecarPath);
    }

    if (allocatedClaim != null && Files.exists(allocatedClaim)) {
      Files.delete(allocatedClaim);
    }
  }

  @Test
  void givenFileInfo_whenAllocateFileOnTmpFolder_thenAllocateFile() throws IOException {

    // given
    Files.createDirectories(Path.of(TEMP_FOLDER));
    final Peer peer = Peer.builder().build();
    final HashMap<String, Peer> peersMap = new HashMap<>();
    peersMap.put(peer.getId(), peer);

    final String hash = Base64.getEncoder().encodeToString("hash_fake_file".getBytes());
    final File file = File.builder()
        .hash(Hash.builder().value(hash).build())
        .names(Set.of("file_name_fake.txt"))
        .size(SIZE_3GB)
        .peers(peersMap)
        .build();

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    // when
    fileAllocatorService.allocateFilePhysical(file);

    // then
    allocatedFilePath = Path.of(TEMP_FOLDER, file.getHash().getValue());
    assertTrue(Files.exists(allocatedFilePath));
    assertEquals(file.getHash().getValue(), allocatedFilePath.getFileName().toString());
  }

  @Test
  void givenFileInfo_whenNoWritePermissions_thenThrowException() throws IOException {
    // given
    Files.createDirectories(Path.of(TEMP_FOLDER));
    final Peer peer = Peer.builder().build();
    final HashMap<String, Peer> peersMap = new HashMap<>();
    peersMap.put(peer.getId(), peer);

    final String hash = Base64.getEncoder().encodeToString("hash_fake_file".getBytes());
    final File file = File.builder()
        .hash(Hash.builder().value(hash).build())
        .names(Set.of("file_name_fake.txt"))
        .size(SIZE_3GB)
        .peers(peersMap)
        .build();

    // when


    // then
    assertThrows(RuntimeException.class, () ->
        fileAllocatorService.allocateFilePhysical(file));
  }

  @Test
  void givenFileInfo_whenAllocateSidecarFile_thenCreateSidecarFile() throws IOException {

    // given
    Files.createDirectories(Path.of(TEMP_FOLDER));
    final Peer peer = Peer.builder().build();
    final HashMap<String, Peer> peersMap = new HashMap<>();
    peersMap.put(peer.getId(), peer);

    final String hash = Base64.getEncoder().encodeToString("hash_fake_file".getBytes());
    final File file = File.builder()
        .hash(Hash.builder().value(hash).build())
        .names(Set.of("file_name_fake.txt"))
        .size(SIZE_3GB)
        .peers(peersMap)
        .build();

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    // when
    fileAllocatorService.allocateSidecar(file);

    //then
    allocatedSidecarPath = Path.of(TEMP_FOLDER, file.getHash().getValue() + EXTENSION_SIDECAR_XUT);
    assertTrue(Files.exists(allocatedSidecarPath));
    assertEquals(file.getHash().getValue() + EXTENSION_SIDECAR_XUT,
        allocatedSidecarPath.getFileName().toString());

  }

  @Test
  void givenFileHash_whenGetFileFragment_thenReturnFragmentBytes() throws IOException {

    final File file = FileTransferSupport.buildFile(TEMP_FOLDER + "/" + HASH_DEFAULT, SIZE_3GB);

    // allocate the file on FS
    Path filePath = Path.of(TEMP_FOLDER, file.getHash().getValue());
    allocateFile(file.getSize(), filePath);

    when(filesPort.findByHash(file.getHash().getValue())).thenReturn(file);

    // when
    final byte[] fileFragment = fileAllocatorService.getFileFragment(file.getHash(), 0);

    // then
    assertEquals(SIZE_8MB, fileFragment.length);

  }

  @Test
  void givenSidecarFile_whenLoadSidecarBitset_thenReturnValidSidecar() throws IOException {
//    int totalFragments = 256;
    // Given
    final Sidecar sidecarFake = FileTransferSupport.buildSidecar(2 * 1024 * 1024).build();
    Files.createDirectories(Path.of(TEMP_FOLDER));
    String hashValue = "valid_sidecar_hash";
    Path sidecarPath = Path.of(TEMP_FOLDER, hashValue + EXTENSION_SIDECAR_XUT);

    allocateSidecarFile(sidecarFake, sidecarPath);

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    // When
    Sidecar sidecar = fileAllocatorService.loadSidecarBitset(
        Hash.builder().value(hashValue).build());

    // Then
    assertEquals(1, sidecar.getVersion());
    assertEquals(sidecarFake.getTotalFragments(), sidecar.getTotalFragments());
    assertEquals(new BitSet(sidecarFake.getTotalFragments()), sidecar.getData());
  }


  @Test
  void givenNonExistentSidecarFile_whenLoadSidecarBitset_thenThrowException() {
    // Given
    String nonExistentHash = "nonexistent_sidecar";

    // When

    // Then
    assertThrows(RuntimeException.class, () -> {
      fileAllocatorService.loadSidecarBitset(Hash.builder().value(nonExistentHash).build());
    });
  }

  @Test
  void givenCorruptedSidecarHeader_whenLoadSidecarBitset_thenThrowIOException() throws IOException {
    // Given
    Files.createDirectories(Path.of(TEMP_FOLDER));
    String corruptedHash = "corrupted_sidecar";
    Path sidecarPath = Path.of(TEMP_FOLDER, corruptedHash + EXTENSION_SIDECAR_XUT);

    // Write a corrupted sidecar header with invalid bytes length
    try (OutputStream out = Files.newOutputStream(sidecarPath, StandardOpenOption.CREATE,
        StandardOpenOption.WRITE)) {
      out.write(1); // Version
      out.write(new byte[SIDECAR_HEADER_FRAGMENTS - 1]); // Corrupted fragments header
    }

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);
    // When
    // Then
    assertThrows(IOException.class, () -> {
      fileAllocatorService.loadSidecarBitset(Hash.builder().value(corruptedHash).build());
    });
  }

  @Test
  void givenSidecar_whenUpdateSidecar_thenSidecarIsUpdated() throws IOException {

    // Given
    final Sidecar sidecarFake = FileTransferSupport.buildSidecar(SIZE_3GB).build();
    final Sidecar sidecarModified = FileTransferSupport.buildSidecar(SIZE_3GB).build();
    final Hash hash = Hash.builder().value(HASH_DEFAULT).build();

    final Path sidecarFilePath = Path.of(TEMP_FOLDER, hash.getValue() + EXTENSION_SIDECAR_XUT);
    allocateSidecarFile(sidecarFake, sidecarFilePath);
    final BitSet bitSetModified = new BitSet(sidecarFake.getTotalFragments());
    bitSetModified.set(0);
    final Sidecar buildSidecarModified = sidecarModified.toBuilder().data(bitSetModified).build();

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    // When
    fileAllocatorService.updateSidecar(buildSidecarModified, hash);

    // then
    assertEquals(bitSetModified, fileAllocatorService.loadSidecarBitset(hash).getData());


  }

  @Test
  void givenHashAndIndex_whenTryClaim_thenReturnTrue() {

    int fragmentIndex = 1;
    final Hash hash = Hash.builder().value(HASH_DEFAULT).build();
    allocatedClaim = Path.of(TEMP_FOLDER, hash.getValue() + EXTENSION_LOCKS,
        Integer.toString(fragmentIndex));

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    final boolean result = fileAllocatorService.tryClaim(hash, fragmentIndex);

    assertTrue(result);

  }

  @Test
  void givenHashAndIndex_whenTryClaim_thenReturnFalseIfAlreadyAllocated() {
    int fragmentIndex = 1;
    final Hash hash = Hash.builder().value(HASH_DEFAULT).build();
    allocatedClaim = Path.of(TEMP_FOLDER, hash.getValue() + EXTENSION_LOCKS,
        Integer.toString(fragmentIndex));

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);
    fileAllocatorService.tryClaim(hash, fragmentIndex); // allocated claim before
    boolean result = fileAllocatorService.tryClaim(hash, fragmentIndex); // allocated claim before

    assertFalse(result);

  }

  @Test
  void givenHashAndIndex_whenDeleteClaim_thenClaimIsDeleted() {
    int fragmentIndex = 1;
    final Hash hash = Hash.builder().value(HASH_DEFAULT).build();

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    fileAllocatorService.tryClaim(hash, fragmentIndex);

    fileAllocatorService.deleteClaim(hash);
    assertFalse(Files.exists(Path.of(TEMP_FOLDER, hash.getValue() + EXTENSION_LOCKS)));
  }

  @Test
  void givenFileFragment_whenGetChunkByFragment_thenReturnValidChunk() {
    int chunkIndex = 4;
    int byteIndex = chunkIndex * CHUNK_SIZE;
    final byte[] bytesData = new byte[8 * 1024 * 1024];
    new Random().nextBytes(bytesData);

    final FileFragment fileFragment = FileTransferSupport.buildFileFragment(bytesData, chunkIndex,
        HASH_DEFAULT);

    final byte[] chunkByFragment = fileAllocatorService.getChunkByFragment(
        fileFragment.getFragmentData(), chunkIndex);

    for (int i = 0; i < CHUNK_SIZE; i++) {

      assertEquals(bytesData[byteIndex + i], chunkByFragment[i]);
    }


  }

  @Test
  void givenFileFragment_whenGetChunkByFragment_thenReturnIndexOutOfBoundsChunk() {
    int chunkIndex = 456465465;
//    int byteIndex = chunkIndex * CHUNK_SIZE;
    final byte[] bytesData = new byte[8 * 1024 * 1024];
    new Random().nextBytes(bytesData);

    final FileFragment fileFragment = FileTransferSupport.buildFileFragment(bytesData, chunkIndex,
        HASH_DEFAULT);


    assertThrows(IllegalArgumentException.class, () ->
        fileAllocatorService.getChunkByFragment(fileFragment.getFragmentData(), chunkIndex));

  }


  @Test
  void givenChunks_whenAssembleFragment_thenReturnValidByteBuffer() {

    final byte[] fragmentData = new byte[8 * 1024 * 1024];
    new Random().nextBytes(fragmentData);
    final ByteBuffer sampleDataBuffer = ByteBuffer.wrap(fragmentData);

    // given

    final FileFragment f = FileTransferSupport.buildFileFragment(fragmentData, 0,
        HASH_DEFAULT);
    final FileFragment fileFragmentTemplate = f.toBuilder().fragmentData(null).build();
    final int chunkCount = (int) ((fileFragmentTemplate.getSize() + CHUNK_SIZE - 1L) / CHUNK_SIZE);

    final ConcurrentHashMap<Integer, FileChunk> chunks = new ConcurrentHashMap<>();
    for (int i = 0; i < chunkCount; i++) {
      final ByteBuffer duplicate = sampleDataBuffer.duplicate().position(i * CHUNK_SIZE);
      final byte[] chunkData = new byte[Math.min(CHUNK_SIZE, duplicate.remaining())];
      duplicate.get(chunkData);
      final FileChunk fileChunk = FileTransferSupport.buildFileChunk(chunkData, i);
      chunks.put(i, fileChunk);
    }

    final ByteBuffer result = fileAllocatorService.assembleFragment(chunks,
        fileFragmentTemplate);

    assertEquals(fragmentData.length, result.capacity());
    assertEquals(sampleDataBuffer, result);
    assertEquals(fileFragmentTemplate.getSize(), result.limit());

  }


  @Test
  void givenFragmentAndFile_whenWriteFragmentToFile_thenWriteSuccessfullyAndReturnFalseIfNotLast()
      throws IOException {
    // given
    int FRAGMENT_POSITION = 0;
    long fileSize = SIZE_3GB;

    final byte[] fragmentData = new byte[8 * 1024 * 1024];
    new Random().nextBytes(fragmentData);
    final ByteBuffer sampleFragmentDataBuffer = ByteBuffer.wrap(fragmentData);
    final Hash hashSample = Hash.builder().value(HASH_DEFAULT).build();

    final FileFragment f = FileTransferSupport.buildFileFragment(fragmentData,
        FRAGMENT_POSITION,
        HASH_DEFAULT);
    final FileFragment fileFragmentTemplate = f.toBuilder().fragmentData(null).build();

    final File fileDownloading = File.builder()
        .size(fileSize)
        .hash(hashSample)
        .build();

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    fileAllocatorService.allocateFilePhysical(fileDownloading);
    allocatedFilePath = Path.of(TEMP_FOLDER, HASH_DEFAULT);

    // allocate sidecar
    final boolean createdSidecar = fileAllocatorService.allocateSidecar(fileDownloading);
    assertTrue(createdSidecar);
    allocatedSidecarPath = Path.of(TEMP_FOLDER,
        fileDownloading.getHash().getValue() + EXTENSION_SIDECAR_XUT);

    // when
    final boolean isLastFragment = fileAllocatorService
        .writeFragmentToFile(fileFragmentTemplate, sampleFragmentDataBuffer, hashSample);

    assertFalse(isLastFragment);

  }

  @Test
  void givenFileFragments_whenWriteFragmentToFile_ASYNC_thenProcessAllFragmentsAndReturnLastOne()
      throws IOException {

    // change temp folder

    // BUILDING FILES WITH REAL RANDOM DATA
    int fileSize = SIZE_128MB;
    final byte[] fileData = new byte[(int) fileSize];
    new Random().nextBytes(fileData);
    final ByteBuffer fileDataBuffer = ByteBuffer.wrap(fileData);
    final String fileMD5 = FileProcessUtils.checksumMD5FromByteBuffer(fileDataBuffer, fileSize);
    final Hash hashFile = Hash.builder().value(fileMD5).build();

    // host
    final Path HOSTFileStoredFolder = Path.of(STORED_FOLDER);
    Files.createDirectories(HOSTFileStoredFolder);
    final Path HOSTFilePath = Path.of(HOSTFileStoredFolder.toString(), hashFile.getValue());
    Files.write(HOSTFilePath, fileData);
    final File HOSTFile = FileTransferSupport.buildFileWithHash(
        STORED_FOLDER + "/" + hashFile.getValue(), fileSize,
        hashFile.getValue()).toBuilder().status(STORED).build();

    // client
    final File CLIENTFile = FileTransferSupport
        .buildFile(Path.of(TEMP_FOLDER + "/" + hashFile.getValue()).toString(),
            fileSize)
        .toBuilder()
        .hash(hashFile)
        .status(DOWNLOADING)
        .build();
    doNothing().when(filesPort).save(CLIENTFile);

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    fileAllocatorService.allocateFilePhysical(HOSTFile);
    fileAllocatorService.allocateSidecar(HOSTFile);

    // building a fragment pool and random order
    int fragmentsCount = fileSize / FRAGMENT_SIZE;
    when(filesPort.findByHash(hashFile.getValue())).thenReturn(HOSTFile);
    ArrayList<FileFragment> fragmentList = new ArrayList<>();

    for (int i = 0; i < fragmentsCount; i++) {
      final byte[] fileFragmentData = fileAllocatorService.getFileFragment(hashFile, i);
      final ByteBuffer fragmentBuffer = ByteBuffer.wrap(fileFragmentData);
      final String fragmentMD5 = FileProcessUtils.checksumMD5FromByteBuffer(fragmentBuffer,
          fileFragmentData.length);
      final FileFragment fragment = FileTransferSupport.buildFileFragment(fileFragmentData, i,
          fragmentMD5).toBuilder().size(fileFragmentData.length).build();
      fragmentList.add(fragment);
    }
    Collections.shuffle(fragmentList); // random order

    List<FileFragment> lastFragment = new CopyOnWriteArrayList<>();

    // ASYNC OPTION
    try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {

      fragmentList.forEach(fragment -> executorService.execute(() -> {
        boolean isLastFragment = fileAllocatorService.writeFragmentToFile(fragment,
            ByteBuffer.wrap(fragment.getFragmentData()), hashFile);

        if (isLastFragment) {
          lastFragment.add(fragment);
        }
      }));

    }
    assertEquals(1, lastFragment.size());
  }

  @Test
  void givenFileFragments_whenWriteFragmentToFile_SYNC_thenProcessAllFragmentsAndReturnLastOne()
      throws IOException {

    // change temp folder

    // BUILDING FILES WITH REAL RANDOM DATA
    int fileSize = SIZE_128MB;
    final byte[] fileData = new byte[(int) fileSize];
    new Random().nextBytes(fileData);
    final ByteBuffer fileDataBuffer = ByteBuffer.wrap(fileData);
    final String fileMD5 = FileProcessUtils.checksumMD5FromByteBuffer(fileDataBuffer, fileSize);
    final Hash hashFile = Hash.builder().value(fileMD5).build();

    // host
    final Path HOSTFileStoredFolder = Path.of(STORED_FOLDER);
    Files.createDirectories(HOSTFileStoredFolder);
    final Path HOSTFilePath = Path.of(HOSTFileStoredFolder.toString(), hashFile.getValue());
    Files.write(HOSTFilePath, fileData);
    final File HOSTFile = FileTransferSupport.buildFileWithHash(
        STORED_FOLDER + "/" + hashFile.getValue(), fileSize,
        hashFile.getValue()).toBuilder().status(STORED).build();

    // client
    final File CLIENTFile = FileTransferSupport
        .buildFile(Path.of(TEMP_FOLDER + "/" + hashFile.getValue()).toString(),
            fileSize)
        .toBuilder()
        .hash(hashFile)
        .status(DOWNLOADING)
        .build();
    doNothing().when(filesPort).save(CLIENTFile);


    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    fileAllocatorService.allocateFilePhysical(HOSTFile);
    fileAllocatorService.allocateSidecar(HOSTFile);

    // building a fragment pool and random order
    int fragmentsCount = fileSize / FRAGMENT_SIZE;
    when(filesPort.findByHash(hashFile.getValue())).thenReturn(HOSTFile);
    ArrayList<FileFragment> fragmentList = new ArrayList<>();

    for (int i = 0; i < fragmentsCount; i++) {
      final byte[] fileFragmentData = fileAllocatorService.getFileFragment(hashFile, i);
      final ByteBuffer fragmentBuffer = ByteBuffer.wrap(fileFragmentData);
      final String fragmentMD5 = FileProcessUtils.checksumMD5FromByteBuffer(fragmentBuffer,
          fileFragmentData.length);
      final FileFragment fragment = FileTransferSupport.buildFileFragment(fileFragmentData, i,
          fragmentMD5).toBuilder().size(fileFragmentData.length).build();
      fragmentList.add(fragment);
    }
    Collections.shuffle(fragmentList); // random order

    List<FileFragment> lastFragment = new CopyOnWriteArrayList<>();

    // SYNC OPTION
    fragmentList.forEach(fragment -> {
      boolean isLastFragment = fileAllocatorService.writeFragmentToFile(fragment,
          ByteBuffer.wrap(fragment.getFragmentData()), hashFile);

      if (isLastFragment) {
        lastFragment.add(fragment);
      }
    });

    assertEquals(1, lastFragment.size());
  }

  @Test
  void givenFragmentAndFile_whenWriteFragmentToFile_thenWriteSuccessfullyAndReturnTrueIfLast()
      throws IOException {
    // given
    int FRAGMENT_POSITION = 0;
    long fileSize = SIZE_8MB;

    final byte[] fragmentData = new byte[8 * 1024 * 1024];
    new Random().nextBytes(fragmentData);
    final ByteBuffer sampleFragmentDataBuffer = ByteBuffer.wrap(fragmentData);
    final Hash hashSample = Hash.builder().value(HASH_DEFAULT).build();

    final FileFragment f = FileTransferSupport.buildFileFragment(fragmentData,
        FRAGMENT_POSITION,
        HASH_DEFAULT);
    final FileFragment fileFragmentTemplate = f.toBuilder().fragmentData(null).build();

    final File fileDownloading = File.builder()
        .size(fileSize)
        .hash(hashSample)
        .build();


    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    fileAllocatorService.allocateFilePhysical(fileDownloading);
    allocatedFilePath = Path.of(TEMP_FOLDER, HASH_DEFAULT);

    // allocate sidecar
    final boolean createdSidecar = fileAllocatorService.allocateSidecar(fileDownloading);
    assertTrue(createdSidecar);
    allocatedSidecarPath = Path.of(TEMP_FOLDER,
        fileDownloading.getHash().getValue() + EXTENSION_SIDECAR_XUT);


    // when
    final boolean isLastFragment = fileAllocatorService
        .writeFragmentToFile(fileFragmentTemplate, sampleFragmentDataBuffer, hashSample);

    assertTrue(isLastFragment);
  }


  @Test
  void givenValidFile_whenFinalizeFile_thenMoveFileToDownloadFolder() throws IOException {

    // Given
    final long fileSize = SIZE_1GB;

    // allocate the file on FS

    // crete dirty file
    File file = FileTransferSupport.buildFileWithHash(TEMP_FOLDER + "/" + HASH_DEFAULT,
        fileSize, "c698c87fb53058d493492b61f4c74189");

    // dirtyPath only to calculate hash
    Path filePathDirty = Path.of(TEMP_FOLDER, file.getHash().getValue());
    allocateFile(file.getSize(), filePathDirty);

    // calculate hash from the dirty path and set the result to real-file
    final String realHashStr = FileProcessUtils.checksumMD5String(filePathDirty);
    file = file.toBuilder().hash(Hash.builder().value(realHashStr).build()).build();

    // delete the dirty file
    Files.delete(filePathDirty);

    // create the clean path with the real hash and allocate CLEAN file
    Path filePathClean = Path.of(TEMP_FOLDER, file.getHash().getValue());
    allocateFile(file.getSize(), filePathClean);

    // allocate sidecar
    final int sidecarSize = (int) ((fileSize + FRAGMENT_SIZE - 1L) / FRAGMENT_SIZE);
    final Sidecar sidecarFake = FileTransferSupport.buildSidecar(sidecarSize).build();
    Path sidecarPath = Path.of(TEMP_FOLDER, file.getHash().getValue() + EXTENSION_SIDECAR_XUT);
    allocateSidecarFile(sidecarFake, sidecarPath);

    // file transfer data from host (with real hash)
    Hash realHash = Hash.builder().value(realHashStr).build();
    FileTransferData fileTransferData = FileTransferData.builder()
        .messageType(MessageType.FILE_CHUNK).hash(realHash).build();

    // mock the repository
    when(filesPort.findByHash(file.getHash().getValue())).thenReturn(file);

    // create directories before mv
    Files.createDirectories(Path.of(DOWNLOAD_FOLDER));

    final Directories directories = new Directories(List.of(), DOWNLOAD_FOLDER, TEMP_FOLDER);
    when(settings.getDirectories()).thenReturn(directories);

    // When
    fileAllocatorService.finalizeFile(fileTransferData);

    // Then
    final File fileModified = file.toBuilder().status(STORED).build();
    verify(filesPort).save(fileModified);
    final Path downloadedPath = Path.of(DOWNLOAD_FOLDER,
        file.getNames().stream().findFirst().orElse(""));
    assertTrue(Files.exists(downloadedPath));
    final String downloadedFileHash = FileProcessUtils.checksumMD5String(downloadedPath);
    assertEquals(file.getHash().getValue(), downloadedFileHash);
    log.info("File expected hash: [{}] - Result file hash: [{}]", file.getHash().getValue(),
        downloadedFileHash);

  }

  public static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private void allocateFile(long size, Path filePath) throws IOException {

    Files.createDirectories(Path.of(TEMP_FOLDER));
    try (FileChannel ch = FileChannel.open(filePath,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING)) {

      if (size > 0) {
        ch.position(size - 1);
        ch.write(ByteBuffer.wrap(new byte[]{0})); // escribe el último byte
        ch.force(true); // opcional, para ver el tamaño inmediatamente
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private void allocateSidecarFile(Sidecar sidecar, Path sidecarPath) throws IOException {
    ByteBuffer header = ByteBuffer.allocate(
            SIDECAR_HEADER_VERSION + SIDECAR_HEADER_FRAGMENTS + SIDECAR_HEADER_PAYLOAD)
        .order(ByteOrder.BIG_ENDIAN);
    header.put((byte) 1); // Version
    header.putInt(sidecar.getTotalFragments()); // Total fragments
    byte[] payload = new BitSet(sidecar.getTotalFragments()).toByteArray();
    header.putInt(payload.length); // Payload length

    try (OutputStream out = Files.newOutputStream(sidecarPath, StandardOpenOption.CREATE,
        StandardOpenOption.WRITE)) {
      out.write(header.array());
      out.write(payload);
    }
  }

}
