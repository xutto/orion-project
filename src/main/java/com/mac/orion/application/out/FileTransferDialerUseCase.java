package com.mac.orion.application.out;

import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Peer;

public interface FileTransferDialerUseCase {

  void sendInitialDownloadFileProcess(Hash hash, Peer targetPeer);

}
