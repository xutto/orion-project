package com.mac.orion.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mac.orion.BaseUnitTest;
import com.mac.orion.application.in.FilesScanExtractorUseCase;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javafx.scene.control.CheckBoxTreeItem;
import org.junit.jupiter.api.Test;

class FilesScanExtractorServiceTest extends BaseUnitTest {

  @Test
  void givenValidPaths_whenDetermineRoot_thenReturnValidRoot() {
    Map<Path, CheckBoxTreeItem<Path>> selectedItems = new HashMap<>();

    final Path path = Path.of("C:\\home");
    final CheckBoxTreeItem<Path> pathCheckBoxTreeItem = new CheckBoxTreeItem<>(path);

    final Path path2 = Path.of("C:\\home\\jaluque\\orangePanda");
    final CheckBoxTreeItem<Path> pathCheckBoxTreeItem2 = new CheckBoxTreeItem<>(path2);

    final Path path3 = Path.of("C:\\home\\jaluque\\bluepanda");
    final CheckBoxTreeItem<Path> pathCheckBoxTreeItem3 = new CheckBoxTreeItem<>(path3);

    final Path path4 = Path.of("D:\\Downloads");
    final CheckBoxTreeItem<Path> pathCheckBoxTreeItem4 = new CheckBoxTreeItem<>(path4);

    final Path path5 = Path.of("D:\\Shared");
    final CheckBoxTreeItem<Path> pathCheckBoxTreeItem5 = new CheckBoxTreeItem<>(path5);
    selectedItems.put(path, pathCheckBoxTreeItem);
    selectedItems.put(path5, pathCheckBoxTreeItem5);
    selectedItems.put(path3, pathCheckBoxTreeItem3);
    selectedItems.put(path2, pathCheckBoxTreeItem2);
    selectedItems.put(path4, pathCheckBoxTreeItem4);

    FilesScanExtractorUseCase useCase = new FilesScanExtractorService();
    final Set<Path> paths = useCase.determineSelectedRootParent(selectedItems.keySet());

    assertEquals(3, paths.size());
    assertTrue(paths.contains(path4));
    assertTrue(paths.contains(path));
    assertTrue(paths.contains(path5));
  }

  //    for (Path p1 : pathsSelected) {
//
//      Path rootCandidate = p1;
//      for (Path p2 : pathsSelected) {
//
//        if (p1.startsWith(p2) && !p2.equals(p1)) {
//          return rootCandidate = determineSelectedRootParent(p2);
//          break;
//        }
//
//      }
//      roots.add(rootCandidate);
//
//    }

//    roots.add(pathInitialized);
}
