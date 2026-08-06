package com.mac.orion.application.in;

import java.util.UUID;

public interface SearchFileUseCase {

  UUID exploreAndSearchFileByTerm(String term, UUID searchId);

}
