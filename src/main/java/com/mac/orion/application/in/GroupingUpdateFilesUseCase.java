package com.mac.orion.application.in;

import com.mac.orion.domain.model.File;
import java.util.Set;

public interface GroupingUpdateFilesUseCase {

  File updateFilePeersByLastSeenIfExisting(File fileParam, File fileLocal);

  boolean updateNamesIfExisting(File fileParam, File fileLocal);

  Set<File> groupFilesBySize(Set<File> files);

}
