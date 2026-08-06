package com.mac.orion.application.out;

import com.mac.orion.domain.model.Peer;

public interface FileSharerDialerUseCase {

  void sendSharedFiles(Peer targetPeer);

}
