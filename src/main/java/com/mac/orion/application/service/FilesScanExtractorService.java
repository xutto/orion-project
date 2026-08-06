package com.mac.orion.application.service;

import com.mac.orion.application.in.FilesScanExtractorUseCase;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FilesScanExtractorService implements FilesScanExtractorUseCase {

  @Override
  public Set<Path> determineSelectedRootParent(Set<Path> selectedItems) {

    Set<Path> roots = new LinkedHashSet<>();

    final Set<Path> pathsSelected = selectedItems
        .stream()
        .sorted(Comparator.comparing(Path::getNameCount))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    for (Path p : pathsSelected) {
      if (!hasParentIn(p, roots)) {
        roots.add(p);
      }
    }

    return roots;
  }

  private boolean hasParentIn(Path path, Set<Path> roots) {
    Path parent = path.getParent();

    while (parent != null) {

      if (roots.contains(parent)) {
        return true;
      }

      parent = parent.getParent();
    }

    return false;
  }


}
