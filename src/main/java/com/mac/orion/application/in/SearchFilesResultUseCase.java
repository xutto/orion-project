package com.mac.orion.application.in;

import com.mac.orion.domain.model.SearchResult;

public interface SearchFilesResultUseCase {

  void sendToUISearchResults(SearchResult searchResult);

}
