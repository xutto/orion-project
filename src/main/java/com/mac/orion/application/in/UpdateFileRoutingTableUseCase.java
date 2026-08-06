package com.mac.orion.application.in;

import com.mac.orion.domain.model.File;
import java.util.Set;

public interface UpdateFileRoutingTableUseCase {


  void updateFileRoutingTable(Set<File> files);
}
