package com.mac.orion.infrastructure.ui.demo;

import com.mac.orion.application.in.FilesScanExtractorUseCase;
import com.mac.orion.application.service.FilesScanExtractorService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mac.orion.domain.share.NodesIdentifierConstants.DUMMY_PATH_IDENTIFIER;

@Slf4j
public class DemoController implements Initializable {

  @FXML
  TreeView<Item> treeView;
  @FXML
  Button saveButton;
  @FXML
  VBox sharedTreeViewContainer;


  private final Map<Path, CheckBoxTreeItem<Item>> finalSelectedPaths = new HashMap<>();

  private static final Set<Path> originSelectedPaths = Set.of(
      Path.of("C:\\home\\jaluque\\keycloak-local").toAbsolutePath().normalize()
  );
  private static final Set<Path> originPartialPaths = Set.of(
      Path.of("C:\\").toAbsolutePath().normalize(),
      Path.of("C:\\home").toAbsolutePath().normalize(),
      Path.of("C:\\home\\jaluque").toAbsolutePath().normalize()
  );

  @Override
  public void initialize(URL location, ResourceBundle resources) {

//    saveButton.setOnMouseClicked(event -> {
//      finalSelectedPaths.values().forEach(item -> {
//        final String status;
//        if(item.isSelected()){
//          status = "selected";
//        } else if(item.isIndeterminate()){
//          status = "indeterminate";
//        }else {
//          status = "unselected";
//        }
//
//        log.info("item: {} - status: [{}]", item.getValue().getDisplayName(), status);
//
//      });
//    });

    //- ----------- - - ----------------------------
    //- ----------- - - ----------------------------
    //- ----------- - - ----------------------------

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

    rootItem.getChildren().setAll(rootItems);

    treeView.setCellFactory(tv -> {
      CheckBoxTreeCell<Item> cell = new CheckBoxTreeCell<>(
          item -> ((CheckBoxTreeItem<Item>) item).selectedProperty(),
          new StringConverter<>() {
            @Override
            public String toString(TreeItem<Item> item) {
              if (item == null || item.getValue() == null) {
                return "";
              }
              return item.getValue().getDisplayName();
            }

            @Override
            public TreeItem<Item> fromString(String text) {
              return null;
            }
          }
      );

      cell.getStyleClass().add("shared-treeview-cell");

      return cell;
    });
    treeView.setRoot(rootItem);
    treeView.setShowRoot(false);

  }

  private CheckBoxTreeItem<Item> createNewItem(Path path) {
    final String displayName =
        path.getFileName() == null ? path.getRoot().toString() : path.getFileName().toString();
    final Item item = new Item(displayName, path, false);
    return new CheckBoxTreeItem<>(item);
  }

  private Set<CheckBoxTreeItem<Item>> createDirectoryTreeItem(final Set<Path> paths,
                                                              CheckBoxTreeItem<Item> rootItem) {

    Set<Path> treeChildren = new HashSet<>();

    for (Path pathSelected : paths) {

      Path pathParent = pathSelected;

      while (pathParent != null && (!pathParent.getRoot().equals(pathParent))) {
        treeChildren.add(pathParent);
        pathParent = pathParent.getParent();
      }

    }
    FilesScanExtractorUseCase useCase = new FilesScanExtractorService();
    final Set<Path> pathsFiltered = useCase.determineSelectedRootParent(treeChildren);

    final Set<CheckBoxTreeItem<Item>> checkBoxTreeItems = treeChildren.stream().map(p -> {
      final CheckBoxTreeItem<Item> treeItemResult = createNewItem(p);
      treeItemResult.setSelected(originSelectedPaths.contains(p));
      treeItemResult.setIndeterminate(originPartialPaths.contains(p));
      return treeItemResult;
    }).collect(Collectors.toSet());

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

  private void generateItemsFromDB(final CheckBoxTreeItem<Item> rootItem) {

    final Set<Path> originSelectedFiltered = originSelectedPaths.stream()
        .filter(Files::exists)
        .filter(Files::isDirectory)
        .collect(Collectors.toSet());

    final Set<CheckBoxTreeItem<Item>> directoryTreeItem = createDirectoryTreeItem(
        originSelectedFiltered, rootItem);

    rootItem.setSelected(originSelectedFiltered.contains(rootItem.getValue().getPath()));
    rootItem.setIndeterminate(originPartialPaths.contains(rootItem.getValue().getPath()));
    rootItem.getChildren().setAll(directoryTreeItem);
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

      finalSelectedPaths.put(item.getValue().getPath(), item);

      finalSelectedPaths.values().forEach(item2 -> {
        final String status;
        if(item2.isSelected()){
          status = "selected";
        } else if(item2.isIndeterminate()){
          status = "indeterminate";
        }else {
          status = "unselected";
        }

        log.info("item: {} - status: [{}]", item.getValue().getDisplayName(), status);

      });


    });

    item.indeterminateProperty().addListener((obs, wasIndeterminate, isIndeterminate) -> {


      finalSelectedPaths.put(item.getValue().getPath(), item);

      finalSelectedPaths.values().forEach(item2 -> {
        final String status;
        if(item2.isSelected()){
          status = "selected";
        } else if(item2.isIndeterminate()){
          status = "indeterminate";
        }else {
          status = "unselected";
        }

        log.info("item: {} - status: [{}]", item.getValue().getDisplayName(), status);

      });


    });


  }

  private void createDirectoryItemChildren(CheckBoxTreeItem<Item> item) {

    final Path path = item.getValue().getPath();

    try (Stream<Path> listStreamFiles = Files.list(path)) {
      final Set<CheckBoxTreeItem<Item>> children = generateDirectoryItems(
          item, listStreamFiles);

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
