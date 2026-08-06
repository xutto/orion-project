package com.mac.orion.application.out;

import com.mac.orion.domain.model.Peer;
import com.mac.orion.domain.model.SearchResource;

public interface FileSearcherDialerUseCase {

  void sendSearchFile(SearchResource searchResource, Peer targetPeer);

}
