package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_SEARCH_ID;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_BAR;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_TABS;

import com.mac.orion.application.in.GroupingUpdateFilesUseCase;
import com.mac.orion.application.in.Publisher;
import com.mac.orion.application.in.SearchFileUseCase;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.SearchResult;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.loader.SearchResultLoader;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class SearchFileCommand<T extends Event> implements Command<T> {

  private static final ExecutorService searchExecutor = Executors.newVirtualThreadPerTaskExecutor();

  // Use cases and services
  private final SearchFileUseCase searchFileUseCase;
  private final GroupingUpdateFilesUseCase groupingUpdateFilesService;
  private final Publisher<SearchResult> searchFilePublisherService;
  private final SearchResultLoader searchResultLoader;

  // UI components and nodes
  private final ControllerNodes searchControllerNodes;
  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final Map<UUID, Map<Hash, Integer>> indexController = new HashMap<>();
  private final Map<UUID, TableView<File>> resultTables = new HashMap<>();


  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.ON_BOOT, List.of());
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(searchControllerNodes.getNode(SEARCH_BUTTON)));
  }

  @Override
  public void execute(T event) {
    // todo si el fichero encontrado ya lo tiene el ownpeer en su bd/filesystem resaltar el fichero <---- ojo esto!

    // TODO CONTROLAR LAS VECES QUE SE PUEDE CLICKEAR EL BOTON DE BUSCAR, deberia caparse

    final TextField searchBar = searchControllerNodes.getNode(SEARCH_BAR, TextField.class);
    final TabPane searchTabs = searchControllerNodes.getNode(SEARCH_TABS, TabPane.class);
    final String searchTerm = searchBar.getText();
    final UUID searchId = UUID.randomUUID();

    Platform.runLater(() -> {
      try {
        final TableView<File> tableViewNew = searchResultLoader.getTableView(searchId);
        resultTables.put(searchId, tableViewNew);
        final Tab tab = new Tab();
        tab.setContent(tableViewNew);
        tab.setText(searchTerm);
        tab.getProperties().put(PROPERTIES_SEARCH_ID, searchId);
        searchTabs.getTabs().add(tab);
        searchTabs.getSelectionModel().select(tab);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    searchFileUseCase.exploreAndSearchFileByTerm(searchTerm, searchId);

  }

  @Override
  public void execute() {

    final TabPane searchTabs = searchControllerNodes.getNode(SEARCH_TABS, TabPane.class);
//    searchTabs.getTabs().clear();

    searchFilePublisherService.subscribe(OperationsType.SAVE, result -> {

      Platform.runLater(() -> {

        // prevent null files
        final Set<File> files = Optional.ofNullable(result.getFiles()).orElse(new HashSet<>());

        files.forEach(f -> {

          final Map<Hash, Integer> indexBySearchId = Optional.ofNullable(
              indexController.get(result.getId())).orElse(new HashMap<>());
          final TableView<File> fileTableView = resultTables.get(result.getId());

          if (indexBySearchId.containsKey(f.getHash())) {

            // if exists
            final Integer index = indexBySearchId.get(f.getHash());
            final File file = fileTableView.getItems().get(index);

            // update peers and file names
            groupingUpdateFilesService.updateFilePeersByLastSeenIfExisting(f, file);
            groupingUpdateFilesService.updateNamesIfExisting(f, file);

            // update the file
            fileTableView.getItems().set(index, file);

          } else {
            // add if not exists
            final int lastIndex = fileTableView.getItems().size();
            fileTableView.getItems().add(lastIndex, f);
            indexBySearchId.put(f.getHash(), lastIndex);
            indexController.put(result.getId(), indexBySearchId);
          }

          log.info("file found: {}", f.getNames().stream().findFirst().orElse(""));
        });

      });



    });

  }
}
