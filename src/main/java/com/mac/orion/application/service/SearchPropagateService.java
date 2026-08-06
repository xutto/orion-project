package com.mac.orion.application.service;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.application.in.SearchProcessUseCase;
import com.mac.orion.application.in.SearchPropagateUseCase;
import com.mac.orion.application.out.FileResultsDialerUeCase;
import com.mac.orion.domain.Bootable;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.SearchResource;
import com.mac.orion.domain.model.SearchResult;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchPropagateService implements SearchPropagateUseCase, Bootable {

  private final Publisher<SearchResource> searchReceiverPublisherService;
  private final SearchProcessUseCase searchProcessService;
  private final FileResultsDialerUeCase fileResultsDialerUeCase;


  @Override
  public void propagateReceiverHandler(final SearchResource searchResource) {

    // send file results to the peer finder
    final Set<File> filesByHashes = searchProcessService.searchFilesByName(searchResource);
    final Peer peerFinder = searchResource.getPeerFinder();
    final SearchResult searchResult = SearchResult.builder().id(searchResource.getId())
        .files(filesByHashes).build();
    // send
    if (filesByHashes != null && !filesByHashes.isEmpty()) {
      fileResultsDialerUeCase.sendFileSearchResults(searchResult, peerFinder);
    }

    // propagate to routing table close peers to search the file depends on the depth
    if (searchResource.getDepth() > 0) {
      searchProcessService.propagateSearch(searchResource);
    }
  }


  @Override
  public void boot() {
    searchReceiverPublisherService.subscribe(OperationsType.RECEIVER,
        this::propagateReceiverHandler);
  }

}
