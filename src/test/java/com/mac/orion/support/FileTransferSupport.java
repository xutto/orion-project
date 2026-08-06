package com.mac.orion.support;

import static com.mac.orion.application.service.FileAllocatorService.FRAGMENT_SIZE;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Status;
import com.mac.orion.domain.model.transfer.FileAvailability;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileError;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.domain.model.transfer.NextWindow;
import com.mac.orion.domain.model.transfer.Sidecar;
import com.mac.orion.domain.model.transfer.TransferEnd;
import com.mac.orion.infrastructure.persistence.entity.FileEntity;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Set;

public class FileTransferSupport {

  public static FileTransferData buildFileGet() {

    return FileTransferData.builder().messageType(MessageType.FILE_GET)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build())
        .build();
  }

  public static FileTransferData buildFileAvailability() {

    // Supón un archivo de ~100 MB: 104_857_600 bytes
    long totalSize = 104_857_600L;               // ~100 MiB
    int fragmentSize = FRAGMENT_SIZE;          // 8 MiB (ya definido en FileAllocatorService)
    int totalFragments = (int) ((totalSize + fragmentSize - 1L) / fragmentSize); // ceil

    BitSet availability = new BitSet(totalFragments);
    availability.set(0, totalFragments); // todas las piezas disponibles en este host

    FileAvailability fileAvailability = FileAvailability.builder()
        .availability(availability)
        .build();

    return FileTransferData.builder()
        .messageType(MessageType.FILE_AVAILABILITY)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build())
        .size(totalSize)
        .fileAvailability(fileAvailability)
        .build();
  }

  public static FileTransferData buildNextWindow(int index) {

    NextWindow nextWindow = NextWindow.builder()
        .index(index)      // quiere el fragmento #7 (8º, 0‑based)
        .last(false)   // aún no es el último fragmento que necesita
        .build();

    return FileTransferData.builder()
        .messageType(MessageType.NEXT_WINDOW)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build())
        .nextWindow(nextWindow)
        .build();

  }

  public static FileTransferData buildFileChunkClient(int fragmentIndex, int chunkIndex ) {

    // Supón que el cliente ya pidió el fragmento #7 (0‑based) con NEXT_WINDOW
    // y ahora solicita el chunk #3 (de 0..N-1) de ese fragmento.

    FileChunk requestChunk = FileChunk.builder()
        .index(chunkIndex)
        .last(false) // el host decidirá si ese era el último
        .build();

    return FileTransferData.builder()
        .messageType(MessageType.FILE_CHUNK)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build()) // hash del archivo
        .nextWindow(NextWindow.builder()
            .index(fragmentIndex)
            .last(false) // puede ser true si este fragmento es el último que necesita el cliente
            .build())
        .fileChunk(requestChunk)
        .build();
  }

  public static FileTransferData buildFileChunkHost(byte[] fragmentBytes, int chunkIndexProvider, int chunkSize) {
    int fragmentIndex = 7;
//    byte[] fragmentBytes =
//        new byte[8 * 1024 * 1024]; // ejemplo: 8 MiB (si fuera el último podría ser menor)
    // ... rellenado internamente por el host leyendo del FS
//    String md5OfFragment = "c3ab8ff13720e8ad9047dd39466b3c89"; // MD5 del fragmento #7 (ejemplo)

    ByteBuffer assembledFragment = ByteBuffer.wrap(fragmentBytes);
    final String md5OfFragment = FileProcessUtils.checksumMD5FromByteBuffer(assembledFragment,
        fragmentBytes.length);

    FileFragment fileFragment = FileFragment.builder()
        .hash(Hash.builder().value(md5OfFragment).build())
        .index(fragmentIndex)
        .size(fragmentBytes.length)   // tamaño real del fragmento
        .fragmentData(fragmentBytes)  // solo en host para poder extraer los chunks
        .build();

    // Supón chunk de 256 KiB (coincide con tu FileAllocatorService.CHUNK_SIZE)
    //int chunkIndex = 0; // el que pidió el cliente
    byte[] chunkData = new byte[256 * 1024];
    // ... rellenado desde fragmentBytes en offset (chunkIndex * 256 KiB)

    // calcular si es el último chunk de este fragmento
//    int chunkSize = 256 * 1024;
    int totalChunks = (fileFragment.getSize() + chunkSize - 1) / chunkSize; // ceil
    boolean isLast = (chunkIndexProvider == totalChunks - 1);

    FileChunk responseChunk = FileChunk.builder()
        .data(chunkData)
        .index(chunkIndexProvider)
        .last(isLast)
        .build();

    return FileTransferData.builder()
        .messageType(MessageType.FILE_CHUNK)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build()) // hash del archivo
        .nextWindow(NextWindow.builder()
            .index(fragmentIndex)
            .last(
                false) // opcional; el host puede marcar true si sabe que es el último fragmento que el cliente necesita
            .build())
        .fileFragment(fileFragment)
        .fileChunk(responseChunk)
        .build();
  }

  public static FileTransferData buildFileTransferEnd() {
    TransferEnd end = TransferEnd.builder()
        // si tu clase tiene campos (motivo, timestamp, etc.), rellénalos aquí
        .build();

    return FileTransferData.builder()
        .messageType(MessageType.TRANSFER_END)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build())
        .transferEnd(end)
        .build();
  }

  public static FileFragment buildFileFragment(byte[] data, int fragmentIndex, String md5OfFragment) {

    return FileFragment.builder()
        .hash(Hash.builder().value(md5OfFragment).build())
        .index(fragmentIndex)
        .size(data.length)   // tamaño real del fragmento
        .fragmentData(data)  // solo en host para poder extraer los chunks
        .build();

  }

  public static FileTransferData buildFileTransferError() {

    FileError error = FileError.builder()
        .code("CHUNK_OUT_OF_RANGE")
        .message("Requested chunk index 42 is out of bounds for fragment 7")
        .build();

    return FileTransferData.builder()
        .messageType(MessageType.FILE_ERROR)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build())
        .fileError(error)
        .build();
  }

  public static File buildFile(String path, long size) {

    return File.builder()
        .id(40L)
        .hash(Hash.builder().value("2fd4e1c67a2d28fced849ee1bb76e739").build())
        .names(Set.of("fake_file_01.txt"))
        .path(path)
        .size(size)
        .score(0)
        .peers(new HashMap<>())
        .status(Status.STORED)
        .build();

  }

  public static File buildFileWithHash(String path, long size, String hash) {

    // c698c87fb53058d493492b61f4c74189
    final File file = buildFile(path, size);
    return file.toBuilder().hash(Hash.builder().value(hash).build()).status(Status.DOWNLOADING)
        .build();
  }

  public static FileEntity buildFileEntity(String path, long size) {
    final FileEntity fileEntity = new FileEntity();
    fileEntity.setId(40L);
    fileEntity.setHash("2fd4e1c67a2d28fced849ee1bb76e739");
    fileEntity.setName("fake_file_01.txt");
    fileEntity.setPath(path);
    fileEntity.setSize(size);
    fileEntity.setStatus(com.mac.orion.infrastructure.persistence.entity.Status.STORED);
    return fileEntity;
  }

  public static Sidecar.SidecarBuilder buildSidecar(long size) {
    int totalFragments = (int) ((size + FRAGMENT_SIZE - 1L) / FRAGMENT_SIZE);
    BitSet data = new BitSet(totalFragments);
    return Sidecar.builder().version(1).data(data).totalFragments(totalFragments);
  }

  public static FileChunk buildFileChunk(byte[] data, int chunkIndex) {
    return FileChunk.builder()
        .index(chunkIndex)
        .data(data)
        .last(false)
        .build();
  }


}
