package com.mac.orion.application.in;

import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.SearchResource;
import java.util.Set;

public interface SearchProcessUseCase {

  void propagateSearch(SearchResource searchResource);

  Set<File> searchFilesByName(SearchResource searchResource);

}
