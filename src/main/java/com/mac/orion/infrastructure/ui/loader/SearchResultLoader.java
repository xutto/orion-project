package com.mac.orion.infrastructure.ui.loader;


import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_SEARCH_ID;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_TABLE_FX_ID;
import static com.mac.orion.domain.share.NodesIdentifierConstants.UI_RESOURCE_SEARCH_RESULT_FXML;

import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.ui.command.Command;
import com.mac.orion.infrastructure.ui.command.FileSelectionCommand;
import com.mac.orion.infrastructure.ui.controller.SearchResultController;
import com.mac.orion.infrastructure.ui.events.EventsAssembler;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchResultLoader {

  private final ControllerNodes searchResultControllerNodes;
  private final SearchResultController searchResultController;
  private final EventsAssembler eventsAssembler;
  private final FileSelectionCommand<Event> fileSelectionCommand;


  public TableView<File> getTableView(final UUID searchId) throws IOException {
    final FXMLLoader loader = new FXMLLoader(getClass().getResource(UI_RESOURCE_SEARCH_RESULT_FXML));
    loader.setController(searchResultController);
    loader.load();
    final TableView<File> fileTableView = (TableView<File>) loader.getNamespace()
        .get(SEARCH_TABLE_FX_ID);
    fileTableView.getProperties().put(PROPERTIES_SEARCH_ID, searchId); // add search id before added to controller nodes
    searchResultControllerNodes.addNode(fileTableView);
    fileSelectionCommand.configureCompatibility();

    final ArrayList<Command<Event>> command = new ArrayList<>();
    command.add(fileSelectionCommand);

    eventsAssembler.configureCommandEvents(command);
    return fileTableView;
  }


}
