package com.mac.orion.domain.dht;

import com.mac.orion.application.in.GroupingUpdateFilesUseCase;
import com.mac.orion.application.out.FileIndexUseCase;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Peer;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileRoutingDataHashTable implements FileRoutingTable {

  private final FileIndexUseCase fileIndexUseCase;
  private final GroupingUpdateFilesUseCase groupingUpdateFilesService;

  // Table with file-hash > peers-ids
  final ConcurrentHashMap<Hash, File> data = new ConcurrentHashMap<>();

  public FileRoutingDataHashTable(@NotNull final FileIndexUseCase fileIndexUseCase, GroupingUpdateFilesUseCase groupingUpdateFilesService) {
    log.info("FileRoutingDataHasTable created");
    this.groupingUpdateFilesService = groupingUpdateFilesService;
    this.fileIndexUseCase = fileIndexUseCase;
  }


  @Override
  public void addFile(final File file) {
    // el fichero puede venir de 2 maneras diferentes:
    // A:Creado localmente
    // B:Updateado desde otro nodo en el proceso share desde UpdateFileRoutingTableService

    // proceso de agregación de fichero y actualización de peers
    // se recibe un File, si existe, se actualizan sus peers, si no existe se agrega

    Optional.ofNullable(file).ifPresent(fileParam ->
        data.compute(fileParam.getHash(), (k, fileLocal) -> {

          if (fileLocal == null) {
            saveFileToIndex(fileParam);
            return fileParam;
          } else {
            File fileUpdated = groupingUpdateFilesService
                .updateFilePeersByLastSeenIfExisting(fileParam, fileLocal);
            if (groupingUpdateFilesService.updateNamesIfExisting(fileParam, fileUpdated)) {
              saveFileToIndex(fileUpdated);
            }
            return fileUpdated;
          }

        }));

  }

  @Override
  public Set<File> getData() {
    return new HashSet<>(data.values());
  }

  @Override
  public Set<Peer> getPeersByfile(Hash hash) {
    log.info("Get peers by hash: [{}]", hash);
    return Optional.ofNullable(data.get(hash))
        .map(File::getPeers)
        .map(Map::values)
        .map(HashSet::new)
        .orElseGet(HashSet::new);
  }


  @Override
  public void removePeerFromFile(Hash fileHash, Peer peer) {
    log.info("Remove peer from hash: [{}]", fileHash);
    Optional.ofNullable(data.get(fileHash))
        .map(File::getPeers)
        .ifPresent(peers -> {
          peers.remove(peer.getId());
          if (peers.isEmpty()) {
            data.remove(fileHash);
          }
        });
  }

  @Override
  public void clear() {
    // todo implements maintain table
  }

  @Override
  public int peersByFile(Hash hash) {
    return Optional.ofNullable(data.get(hash))
        .map(File::getPeers)
        .map(Map::size)
        .orElse(0);
  }

  @Override
  public Set<File> getFilesByHashes(Set<Hash> hashes) {
    if (hashes == null || hashes.isEmpty()) return Collections.emptySet();
    return hashes.stream()
        .map(data::get)                // O(1) por hash
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private void saveFileToIndex(File file) {
    try {
      fileIndexUseCase.indexFile(file);
    } catch (IOException e) {
      log.error("Error indexing file: {}", file.getPath(), e);
    }
  }
}
