package com.mac.orion.infrastructure.ui.command;


import com.mac.orion.application.in.FilesScanExtractorUseCase;
import com.mac.orion.application.in.SettingsManagementUseCase;
import com.mac.orion.domain.model.settings.Directories;
import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.infrastructure.ui.demo.Item;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mac.orion.domain.share.NodesIdentifierConstants.DUMMY_PATH_IDENTIFIER;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_SHARED_TREEVIEW;

@Slf4j
@RequiredArgsConstructor
@Component
public class SharedFilesTreeCommand implements Command<Event> {

  private final Map<Path, CheckBoxTreeItem<Item>> finalSelectedPaths = new HashMap<>();
  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final Settings settings;
  private final SettingsManagementUseCase settingsManagementUseCase;
  private final FilesScanExtractorUseCase filesScanExtractorUseCase;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.ON_BOOT, List.of());
  }

  @Override
  public void execute() {

    settings.getDirectories().scan().forEach(s -> {
      final Path path = Path.of(s);
      final CheckBoxTreeItem<Item> newOriginItem = createNewItem(path);
      newOriginItem.setSelected(true);
      finalSelectedPaths.put(path, newOriginItem);
    });

    final TreeView<Item> treeView = settingsControllerNodes.getNode(SETTINGS_SHARED_TREEVIEW, TreeView.class);

    File[] rootsDisks = File.listRoots();
    CheckBoxTreeItem<Item> rootItem = new CheckBoxTreeItem<>();

    final Set<CheckBoxTreeItem<Item>> rootItems = Arrays.stream(rootsDisks)
        .map(File::toPath)
        .sorted(Comparator.comparing(Path::toString))
        .map(this::createNewItem)
        .peek(this::generateItemsFromDB)
        .peek(i -> {
          if (!i.getValue().getListening()) {
            createDirectoryItemChildrenOnExpand(i);
          }
        })
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Platform.runLater(() -> {
      rootItem.getChildren().setAll(rootItems);
      treeView.setRoot(rootItem);
      treeView.setShowRoot(false);
    });

  }

  private CheckBoxTreeItem<Item> createNewItem(Path path) {
    final String displayName =
        path.getFileName() == null ? path.getRoot().toString() : path.getFileName().toString();
    final Item item = new Item(displayName, path, false);
    return new CheckBoxTreeItem<>(item);
  }

  private void generateItemsFromDB(final CheckBoxTreeItem<Item> rootItem) {

    final Set<Path> originSelectedPaths = settings.getDirectories()
        .scan()
        .stream()
        .map(Path::of)
        .collect(Collectors.toSet());

    final Set<Path> originSelectedFiltered = originSelectedPaths.stream()
        .filter(Files::exists)
        .filter(Files::isDirectory)
        .collect(Collectors.toSet());

    final Set<CheckBoxTreeItem<Item>> directoryTreeItem = createDirectoryTreeItem(
        originSelectedFiltered, rootItem, originSelectedPaths);

    final boolean rootIsIndeterminate = directoryTreeItem.stream()
        .anyMatch(cht -> cht.getValue().getPath().getRoot().equals(rootItem.getValue().getPath()));

    rootItem.setSelected(originSelectedFiltered.contains(rootItem.getValue().getPath()));
    rootItem.setIndeterminate(rootIsIndeterminate);
    rootItem.getChildren().setAll(directoryTreeItem);
  }

  private Set<CheckBoxTreeItem<Item>> createDirectoryTreeItem(final Set<Path> paths,
                                                              CheckBoxTreeItem<Item> rootItem, Set<Path> originSelectedPaths) {

    Set<Path> treeChildren = new HashSet<>();

    for (Path pathSelected : paths) {

      Path pathParent = pathSelected;

      while (pathParent != null && (!pathParent.getRoot().equals(pathParent))) {
        treeChildren.add(pathParent);
        pathParent = pathParent.getParent();
      }

    }

    final Set<Path> pathsFiltered = filesScanExtractorUseCase.determineSelectedRootParent(treeChildren);

    final Set<CheckBoxTreeItem<Item>> checkBoxTreeItems = treeChildren.stream().map(p -> {
          final CheckBoxTreeItem<Item> treeItemResult = createNewItem(p);
          treeItemResult.setIndeterminate(true);
          createDirectoryItemChildrenOnExpand(treeItemResult);
          return treeItemResult;
        })
        .peek(item -> {
          if (originSelectedPaths.contains(item.getValue().getPath())) {
            item.setIndeterminate(false);
            item.setSelected(true);
          }
        })
        .collect(Collectors.toSet());

    checkBoxTreeItems.forEach(item -> {

      if (item.getValue().getPath().getParent() != null) {
        final Path parent = item.getValue().getPath().getParent().normalize();
        checkBoxTreeItems.stream()
            .filter(i -> i.getValue().getPath().normalize().equals(parent))
            .findFirst()
            .ifPresent(item1 -> item1.getChildren().add(item));
      }
    });
    final Set<CheckBoxTreeItem<Item>> filteredCheckBoxItems = checkBoxTreeItems.stream()
        .filter(i -> pathsFiltered.contains(i.getValue().getPath()))
        .filter(i -> i.getValue().getPath().getRoot().equals(rootItem.getValue().getPath()))
        .collect(Collectors.toSet());

    return filteredCheckBoxItems;

  }

  private void createDirectoryItemChildrenOnExpand(CheckBoxTreeItem<Item> item) {

    final CheckBoxTreeItem<Item> itemDummy = new CheckBoxTreeItem<>(
        new Item(DUMMY_PATH_IDENTIFIER, Path.of(DUMMY_PATH_IDENTIFIER), false));
    if (item.getChildren().isEmpty() && !item.isExpanded()) {
      log.debug("adding dummy child to: {} - {}", item.getValue(), item.isExpanded());
      item.getChildren().add(itemDummy);
    }

    item.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
      item.getChildren().remove(itemDummy);
      item.getValue().setListening(true); // set item is listening

      if (isExpanded) {
        log.info("expanding: {} - {}", item.getValue(), item.isExpanded());
        createDirectoryItemChildren(item);
      }
    });

    item.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
      log.info("Directory {} indeterminate: {}", item.isIndeterminate(), item.getValue().getPath());
      log.info("Directory {} selected: {}", item.isSelected(), item.getValue().getPath());

      finalSelectedPaths.put(item.getValue().getPath(), item);

      Set<Path> selectsPaths = finalSelectedPaths.values().stream()
          .filter(CheckBoxTreeItem::isSelected)
          .filter(c -> !c.isIndeterminate())
          .map(checkBoxTreeItem -> checkBoxTreeItem.getValue().getPath())
          .collect(Collectors.toSet());
      final List<String> directoriesFilteredAndDetermineRootParents = filesScanExtractorUseCase.determineSelectedRootParent(selectsPaths)
          .stream()
          .map(p -> p.toAbsolutePath().normalize().toString())
          .toList();


      final Directories directoriesNew = settings.getDirectories().toBuilder()
          .scan(directoriesFilteredAndDetermineRootParents)
          .build();
      settings.setDirectories(directoriesNew);
      settingsManagementUseCase.saveSettings(settings);

    });

    item.indeterminateProperty().addListener((observable, oldValue, newValue) -> {
      log.info("Directory {} indeterminate: {}", item.isIndeterminate(), item.getValue().getPath());
      log.info("Directory {} selected: {}", item.isSelected(), item.getValue().getPath());
      finalSelectedPaths.put(item.getValue().getPath(), item);

      if (item.isSelected()) {
        Set<Path> selectsPaths = finalSelectedPaths.values().stream()
            .filter(CheckBoxTreeItem::isSelected)
            .filter(c -> !c.isIndeterminate())
            .map(checkBoxTreeItem -> checkBoxTreeItem.getValue().getPath())
            .collect(Collectors.toSet());
        final List<String> directoriesFilteredAndDetermineRootParents = filesScanExtractorUseCase.determineSelectedRootParent(selectsPaths)
            .stream()
            .map(p -> p.toAbsolutePath().normalize().toString())
            .toList();


        final Directories directoriesNew = settings.getDirectories().toBuilder()
            .scan(directoriesFilteredAndDetermineRootParents)
            .build();
        settings.setDirectories(directoriesNew);
        settingsManagementUseCase.saveSettings(settings);
      }

    });
  }

  private void createDirectoryItemChildren(CheckBoxTreeItem<Item> item) {

    final Path path = item.getValue().getPath();

    try (Stream<Path> listStreamFiles = Files.list(path)) {
      final Set<CheckBoxTreeItem<Item>> children = generateDirectoryItems(
          item, listStreamFiles);

      children.stream()
          .filter(CheckBoxTreeItem::isSelected)
          .forEach(c -> finalSelectedPaths.put(c.getValue().getPath(), c));

      item.getChildren().setAll(children);

    } catch (IOException e) {
      item.getChildren().clear();
      log.warn("Error listing files", e);
    }

  }

  private Set<CheckBoxTreeItem<Item>> generateDirectoryItems(CheckBoxTreeItem<Item> item,
                                                             Stream<Path> pathStream) {
    final Set<CheckBoxTreeItem<Item>> children = pathStream
        .filter(Files::exists)
        .filter(Files::isDirectory)
        .sorted(Comparator.comparing(Path::toString))
        .map(p -> createDirectoryItem(p, item))
        .peek(i -> {
          if (!i.getValue().getListening()) {
            createDirectoryItemChildrenOnExpand(i);
          }
        })
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return children;
  }

  private CheckBoxTreeItem<Item> createDirectoryItem(Path path, CheckBoxTreeItem<Item> parentItem) {

    return parentItem.getChildren()
        .stream()
        .filter(i -> i.getValue().getPath().toAbsolutePath().normalize().toString()
            .equals(path.toAbsolutePath().normalize().toString()))
        .filter(child -> child.getValue() != null)
        .filter(i -> !parentItem.getChildren().isEmpty())
        .findFirst()
        .map(i -> {
          final CheckBoxTreeItem<Item> itemGen = (CheckBoxTreeItem<Item>) i;
          log.debug("reutilizing item: {}", itemGen.getValue());
          return itemGen;
        })
        .orElseGet(() -> {
          final CheckBoxTreeItem<Item> pathCheckBoxTreeItem = createNewItem(path);
          pathCheckBoxTreeItem.setSelected(
              (parentItem.isSelected() && parentItem.getValue().getPath()
                  .equals(path.getParent())));
          log.debug("creating new item: {}", pathCheckBoxTreeItem.getValue());
          return pathCheckBoxTreeItem;
        });

  }

}
