package com.mac.orion.infrastructure.p2p.controller;

import com.mac.orion.infrastructure.p2p.model.FileTransfer;
import com.mac.orion.infrastructure.p2p.model.FileTransfer.FileMessage;
import java.util.concurrent.CompletableFuture;

public interface FileTransferController extends Controller {



  default CompletableFuture<Boolean> send(FileTransfer.FileMessage message){
    throw new IllegalStateException("Responder only!");
  }

  default CompletableFuture<FileMessage> sendAsync(FileTransfer.FileMessage message){
    throw new IllegalStateException("Responder only!");
  }

}
