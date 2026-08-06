package com.mac.orion.application.out;

import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import java.io.IOException;
import java.util.Set;

public interface FileIndexUseCase {

  void indexFile(File file)  throws IOException;

  Set<Hash> findByNameWithLimit(String name, Integer limit);

}
