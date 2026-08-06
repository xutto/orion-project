package com.mac.orion.application.out;

import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.SearchResult;

public interface FileResultsDialerUeCase {

  void sendFileSearchResults(SearchResult searchResult, Peer finderPeer);
}
