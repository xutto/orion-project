package com.mac.orion.application.service;

import com.mac.orion.application.in.SearchFileUseCase;
import com.mac.orion.application.in.SearchFilesResultUseCase;
import com.mac.orion.application.in.SearchProcessUseCase;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.SearchResource;
import com.mac.orion.domain.model.SearchResult;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchFilesService implements SearchFileUseCase {


  private final SearchProcessUseCase searchProcessService;
  private final SearchFilesResultUseCase searchFilesResultService;
  private final RoutingTable routingTable;


  @Override
  public UUID exploreAndSearchFileByTerm(final String term, UUID searchId) {
    // todo este metodo es el que va a ser llamado por la UI

    // clause term
    if (term == null || term.isEmpty()) {
      return null;
    }

    // clause routing table, own hostnode
    if (routingTable.getHostNodeData().getAddresses().isEmpty()) {
      return null;
    }

    // create own peer
    final Peer ownPeer = Peer.builder()
        .id(routingTable.getHostNodeData().getId())
        .address(routingTable.getHostNodeData().getAddresses())
        .lastSeen(Instant.now())
        .build();

    // create search resource
    final SearchResource searchResource = SearchResource.builder()
        .id(searchId)
        .snippet(term)
        .peerFinder(ownPeer)
        .limit(1000) // TODO CONFIG
        .depth(3) // TODO CONFIG
        .build();

    // get files
    final Set<File> filesByHashes = searchProcessService.searchFilesByName(searchResource);
    final SearchResult searchResult = SearchResult.builder().id(searchResource.getId())
        .files(filesByHashes).build();

    // publish local results
    searchFilesResultService.sendToUISearchResults(searchResult);

    // sending to routing table close peers to search the file
    searchProcessService.propagateSearch(searchResource);


    return searchId;
  }
}
