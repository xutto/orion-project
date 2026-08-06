package com.mac.orion.application.in;

import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Transfer;

public interface TransferControlUseCase {


  Transfer getDataTransfer(Hash hash);

}
