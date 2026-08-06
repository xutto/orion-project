package com.mac.orion.application.service.agent;

import com.mac.orion.domain.model.transfer.FileTransferData;

public interface Agent {

  void audit(FileTransferData fileTransferData);
}
