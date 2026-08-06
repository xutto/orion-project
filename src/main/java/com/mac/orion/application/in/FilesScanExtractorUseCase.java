package com.mac.orion.application.in;

import java.nio.file.Path;
import java.util.Set;

public interface FilesScanExtractorUseCase {

  Set<Path> determineSelectedRootParent(Set<Path> selectedItems);
}
