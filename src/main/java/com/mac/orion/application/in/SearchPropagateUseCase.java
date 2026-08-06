package com.mac.orion.application.in;

import com.mac.orion.domain.model.SearchResource;

public interface SearchPropagateUseCase {

  void propagateReceiverHandler(final SearchResource searchResource);

}
