package com.mac.orion.protoP2P;

import com.mac.orion.application.out.FileIndexUseCase;
import com.mac.orion.domain.dht.FileRoutingTable;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Peer;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

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
//@Component
public class FileRoutingDataHashTableToTest implements FileRoutingTable {

  private final FileIndexUseCase fileIndexUseCase;

  // Table with file-hash > peers-ids
  final ConcurrentHashMap<Hash, File> data = new ConcurrentHashMap<>();

  public FileRoutingDataHashTableToTest(@NotNull final FileIndexUseCase fileIndexUseCase) {
    log.info("[TEST] FileRoutingDataHasTable [TEST] created ");
    this.fileIndexUseCase = fileIndexUseCase;
  }

  @Override
  public void addFile(final File file) {
    log.info("Add to [TEST] local routing table file: [{}]", file);
    Optional.ofNullable(file).ifPresent(fileParam ->
        data.compute(fileParam.getHash(), (k, fileLocal) -> {

          if (fileLocal == null) {
            try {
              fileIndexUseCase.indexFile(fileParam);
            } catch (IOException e) {
              log.error("[TEST] Error indexing file: {}", fileParam.getPath(), e);
            }
            return fileParam;
          } else {
            return updateFilePeersByLastSeenIfExisting(fileParam, fileLocal);
          }

        }));

  }

  @Override
  public Set<File> getData() {
    return new HashSet<>(data.values());
  }

  @Override
  public Set<Peer> getPeersByfile(Hash hash) {
    log.info("Get peers [TEST] by hash: [{}]", hash);
    return new HashSet<>(data.get(hash).getPeers().values());
  }

  @Override
  public Set<File> getFilesByHashes(Set<Hash> hashes) {
    if (hashes == null || hashes.isEmpty()) return Collections.emptySet();
    return hashes.stream()
        .map(data::get)                // O(1) por hash
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
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


  private File updateFilePeersByLastSeenIfExisting(File fileParam, File fileLocal) {
    fileParam.getPeers().forEach((id, p) ->
        fileLocal.getPeers().compute(id, (id1, p1) -> {

              if (p1 == null) {
                return p;
              } else {

                if (!p1.getLastSeen().equals(p.getLastSeen())
                    && p.getLastSeen().isAfter(p1.getLastSeen())) {

                  return p1.toBuilder()
                      .id(id)
                      .lastSeen(p.getLastSeen())
                      .build();

                }
                return p1; // same time, not updated
              }
            }
        ));

    return fileLocal;
  }
}
