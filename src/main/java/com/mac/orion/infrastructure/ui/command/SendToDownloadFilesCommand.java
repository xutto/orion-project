package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.DOWNLOAD_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_SEARCH_ID;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_TABS;

import com.mac.orion.application.in.DownloadFilesInitiatorUseCase;
import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendToDownloadFilesCommand<T extends Event> implements Command<T> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final ControllerNodes searchResultControllerNodes;
  private final ControllerNodes searchControllerNodes;
  private final DownloadFilesInitiatorUseCase downloadFilesInitiatorUseCase;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(searchControllerNodes.getNode(DOWNLOAD_BUTTON)));
  }

  @Override
  public void execute(T event) {
    final TabPane searchTabs = searchControllerNodes.getNode(SEARCH_TABS, TabPane.class);
    final UUID searchId = (UUID) searchTabs.getSelectionModel().getSelectedItem().getProperties()
        .get(PROPERTIES_SEARCH_ID);

    final Optional<TableView<File>> fileTableView = searchResultControllerNodes.getNodes().stream()
        .filter(node -> node instanceof TableView)
        .map(node -> (TableView<File>) node)
        .filter(tableView -> tableView.getProperties().get(PROPERTIES_SEARCH_ID).equals(searchId))
        .findFirst();

    if (fileTableView.isPresent()) {
      final List<File> selectedItems = fileTableView.get().getSelectionModel()
          .getSelectedItems().stream().toList();
      log.info("Sending files to download: {}", selectedItems.size());

      // Triggers the download process for all selected files
      selectedItems.forEach(downloadFilesInitiatorUseCase::downloadFileInitiatorProcess);
    }

    log.info("Executing search id: {}", searchId);
  }

}
