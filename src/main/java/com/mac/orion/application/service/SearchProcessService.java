package com.mac.orion.application.service;

import com.mac.orion.application.in.SearchProcessUseCase;
import com.mac.orion.application.out.FileIndexUseCase;
import com.mac.orion.application.out.FileSearcherDialerUseCase;
import com.mac.orion.domain.dht.FileRoutingDataHashTable;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.SearchResource;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchProcessService implements SearchProcessUseCase {

  private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  private final FileIndexUseCase fileIndexUseCase;
  private final RoutingTable routingTable;
  private final FileSearcherDialerUseCase fileSearcherDialerUseCase;
  private final FileRoutingDataHashTable fileRoutingDataHashTable;

  @Override
  public void propagateSearch(SearchResource searchResource) {
    final SearchResource searchResourceModified = searchResource.toBuilder()
        .depth(searchResource.getDepth() - 1)
        .build();
    routingTable.getAllPeers().forEach(peer ->
        executor.submit(
            () -> fileSearcherDialerUseCase.sendSearchFile(searchResourceModified, peer)));
  }

  @Override
  public Set<File> searchFilesByName(SearchResource searchResource) {
    // search local files in own index
    final Set<Hash> byNameWithLimit = fileIndexUseCase.findByNameWithLimit(
        searchResource.getSnippet(), searchResource.getLimit());
    return fileRoutingDataHashTable.getFilesByHashes(byNameWithLimit);
  }


}
