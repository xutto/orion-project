package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_SEARCH_ID;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_TABS;

import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileSelectionCommand<T extends Event> implements Command<T> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final Map<UUID, List<File>> selectedFiles = new ConcurrentHashMap<>();

  private final ControllerNodes searchResultControllerNodes;
  private final ControllerNodes searchControllerNodes;


  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    final List<Node> list = searchResultControllerNodes
        .getNodes()
        .stream()
        .filter(node -> node instanceof TableView)
        .toList();
    compatibilities.put(EventType.MOUSE_CLICKED, list);

  }

  @Override
  public void execute(T event) {

    final TableView<File> tableView  = (TableView) event.getSource();
    final TabPane tabPane = searchControllerNodes.getNode(SEARCH_TABS, TabPane.class);
    final Tab selectedTab =  tabPane.getSelectionModel().getSelectedItem();
    UUID tabUUID = (UUID) selectedTab.getProperties().get(PROPERTIES_SEARCH_ID);

    switchDownloadButton(tableView.getSelectionModel().getSelectedItems().isEmpty());

  }

  private void switchDownloadButton(boolean disabled){

    final Node node = searchControllerNodes.getNode("download-container");
    node.setDisable(disabled);
  }
}



//    final TableView<File> tableView  = (TableView) event.getSource();
//    final TabPane tabPane = searchControllerNodes.getNode(SEARCH_TABS, TabPane.class);
//    final Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
//    final ObservableList<File> selectedItems = tableView.getSelectionModel().getSelectedItems();
//
//    UUID tabUUID = (UUID) selectedTab.getProperties().get(PROPERTIES_SEARCH_ID);
//
//
//    log.info();
//



//    final int size = tableView.getSelectionModel().getSelectedItems().size();
