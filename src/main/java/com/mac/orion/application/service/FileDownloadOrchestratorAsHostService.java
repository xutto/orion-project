package com.mac.orion.application.service;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.application.in.FileDownloadOrchestratorAsHostUseCase;
import com.mac.orion.application.out.FilesPort;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.transfer.FileAvailability;
import com.mac.orion.domain.model.transfer.FileChunk;
import com.mac.orion.domain.model.transfer.FileFragment;
import com.mac.orion.domain.model.transfer.FileTransferData;
import com.mac.orion.domain.model.transfer.MessageType;
import com.mac.orion.domain.model.transfer.Sidecar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class FileDownloadOrchestratorAsHostService implements
    FileDownloadOrchestratorAsHostUseCase {

  public static final int FIRST_CHUNK_INDEX = 0;
  private final FilesPort filesPort;
  private final FileAllocatorService fileAllocatorService;

  @Override
  public FileTransferData performFileTransfer(FileTransferData fileTransferData,
                                              ConcurrentHashMap<Hash, FileFragment> fragmentData) {

    return switch (fileTransferData.getMessageType()) {
      case FILE_GET -> getFileAvailability(fileTransferData);
      case NEXT_WINDOW -> startFileChunkTransferProcess(fileTransferData, fragmentData);
      case FILE_CHUNK -> continueFileChunkTransferProcess(fileTransferData, fragmentData);
      case TRANSFER_END -> finalizeTransferFileStream(fileTransferData);
      default ->
          fileTransferData; // todo null , controlar en el handler que esto puede ser nulo para da error o MessageType.FILE_ERROR
    };

  }

  // todo se deja este metodo "redundante" para que el futuro pueda contemplar cosas como la puntuación contra el cliente o cosas parecidas
  private FileTransferData finalizeTransferFileStream(FileTransferData fileTransferData) {
    return FileTransferData.builder().messageType(MessageType.TRANSFER_END).build();
  }

  private FileTransferData getFileAvailability(FileTransferData fileTransferData) {

    final File file = filesPort.findByHash(fileTransferData.getHash().getValue());

    if (file != null) {

      final Path path = Path.of(file.getPath());

      if (Files.exists(path)) {

        return switch (file.getStatus()) {
          case STORED -> getAvailabilityFromStored(file);
          case DOWNLOADING -> getAvailabilityFromDownloading(file);
          default -> throw new IllegalStateException("Unexpected value: " + file.getStatus());
        };

      }

    }
    return null;
  }

  private FileTransferData getAvailabilityFromStored(File file){
    // create new bitsetFull with all fragments available
    int totalFragments = fileAllocatorService.getFragmetsBySize(file.getSize());
    final BitSet fullBitSet = fileAllocatorService.createFullBitSet(totalFragments);

    return setFileAvailability(fullBitSet, file);
  }

  private FileTransferData getAvailabilityFromDownloading(File file){
    try {
      final Sidecar sidecar = fileAllocatorService.loadSidecarBitset(file.getHash());
      return setFileAvailability(sidecar.getData(), file);
    } catch (IOException e) {
      log.error("Error loading sidecar bitset", e);
      throw new RuntimeException(e);
    }

  }

  private FileTransferData setFileAvailability(BitSet availability, File file) {
    final FileAvailability fileAvailability = FileAvailability.builder().availability(availability)
        .build();
    return FileTransferData.builder()
        .hash(file.getHash())
        .messageType(MessageType.FILE_AVAILABILITY)
        .size(file.getSize())
        .fileAvailability(fileAvailability)
        .build();
  }


  private FileTransferData startFileChunkTransferProcess(FileTransferData fileTransferData,
                                                         ConcurrentHashMap<Hash, FileFragment> fragmentData) {

    final FileFragment extractedFragment = extractFileFragment(fileTransferData,
        fileTransferData.getNextWindow().getIndex());

    // established fragment first time
    fragmentData.put(fileTransferData.getHash(), extractedFragment);

    return getFileChunkData(fileTransferData, extractedFragment, FIRST_CHUNK_INDEX);
  }

  private FileTransferData continueFileChunkTransferProcess(FileTransferData fileTransferData,
                                                            ConcurrentHashMap<Hash, FileFragment> fragmentData) {

    final FileFragment fileFragment = fragmentData.get(fileTransferData.getHash());
    return getFileChunkData(fileTransferData, fileFragment,
        fileTransferData.getFileChunk().getIndex());
  }

  private FileTransferData getFileChunkData(FileTransferData fileTransferData,
                                            FileFragment fileFragment, Integer chunkIndex) {

    final Hash fileHash = fileTransferData.getHash();
    final byte[] chunkByFragment = fileAllocatorService // 0 represent first fragment in the file
        .getChunkByFragment(fileFragment.getFragmentData(), chunkIndex);

    final int totalChunks = fileAllocatorService.getChunksBySize(fileFragment.getSize());

    final FileChunk fileChunk = FileChunk.builder()
        .data(chunkByFragment)
        .index(chunkIndex)
        .last((chunkIndex == totalChunks - 1))
        .build();

    // rebuild fragment without full data, only some information is valid
    final FileFragment fragmentRebuild =
        FileFragment.builder()
            .hash(fileFragment.getHash())
            .size(fileFragment.getSize())
            .index(fileFragment.getIndex())
            .build();

    return FileTransferData.builder()
        .hash(fileHash)
        .messageType(MessageType.FILE_CHUNK)
        .nextWindow(fileTransferData.getNextWindow()) // next window response
        .fileFragment(fragmentRebuild)
        .fileChunk(fileChunk)
        .build();

  }

  @NotNull
  private FileFragment extractFileFragment(FileTransferData fileTransferData, Integer fragmentIndex) {
    final byte[] fragment = fileAllocatorService.getFileFragment(
        fileTransferData.getHash(),
        fragmentIndex);
    final Hash hash = Hash.builder()
        .value(FileProcessUtils.checksumMD5FromBytes(fragment))
        .build();

    return FileFragment.builder()
        .hash(hash)
        .index(fragmentIndex)
        .size(fragment.length)
        .fragmentData(fragment)
        .build();
  }


}

// todo traer el fragmento
// todo calcular cuantos chunks tiene el fragmento (podria ser el ultimo y no ocupar todos los bytes)
// todo extraer el chunk que nos pide filetranferData
// todo set el index-chunk
// todo set binary-chunk to FileChunk
// todo return FileTransferData con el chunkData el index y el index del FRAGMENTO

//    if (data.get(index) == null) {
//      final byte[] fragment = fileAllocatorService.getFileFragment(fileTransferData.getHash(),
//          index);
//
//      data.put(index, fragment);
//      final byte[] chunkByFragment = fileAllocatorService.getChunkByFragment(fragment,
//          0);// 0 represent first fragment in the file
//    } else {
//      final byte[] fragment = data.get(index);
//      final byte[] chunkByFragment = fileAllocatorService.getChunkByFragment(fragment,
//          0);// 0 represent first fragment in the file
//    }

//    final File foundFile = filesPort.findByHash(fileTransferData.getHash().getValue());

//    if (fileChannel != null) {
//      fileAllocatorService.getFileFragment(fileTransferData.getHash(),
//          index, fileChannel);
//    }

// filechannel

// index fragmento
// hash del archivo

// mapa <hash-index, byte[]>

//    Optional.ofNullable(data.get(fileTransferData.getNextWindow().getIndex())).map(f -> {
//      final byte[] chunkByFragment = fileAllocatorService.getChunkByFragment(fragment,
//          0);
//      // todo return message
//      return null;
//    }).or

//
//    if (fileTransferData.getNextWindow().getLast()) {
//      // todo esto quiere decir que es el último fragmento que necesita el cliente
//    }
//    final Hash fileHash = fileTransferData.getHash();
//    final Integer index = fileTransferData.getNextWindow().getIndex();
//    final FileFragment fileFragment = Optional.ofNullable((fragmentData.get(fileHash)))
//        .orElseGet(() -> {
//
//          final byte[] fragment = fileAllocatorService.getFileFragment(
//              fileTransferData.getHash(),
//              index);
//
//          final FileFragment fileFragmentExtracted =
//              FileFragment.builder()
//                  .fragmentData(fragment)
//                  .index(index)
//                  .build();
//
//          fragmentData.put(fileHash, fileFragmentExtracted);
//          return fileFragmentExtracted;
//        });
//    final byte[] chunkByFragment = fileAllocatorService
//        // 0 represent first fragment in the file
//        .getChunkByFragment(fileFragment.getFragmentData(), FIRST_CHUNK_INDEX);
//
//    final FileChunk fileChunk = FileChunk.builder()
//        .data(chunkByFragment)
//        .index(FIRST_CHUNK_INDEX)
//        .build();
//    return FileTransferData.builder().nextWindow(fileTransferData.getNextWindow())
//        .fileChunk(fileChunk).build();
