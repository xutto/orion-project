package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.APP_TITTLE_TEXT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.CONNECTION_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.FILES_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.TRANSFERS_OPTION_CONTENT;

import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChangeTitleNamesCommand implements Command<Event> {

  public static final String TITTLE_CONNECTION = "Conexión";
  public static final String TITTLE_FILES = "Archivos";
  public static final String TITTLE_SEARCH = "Buscar";
  public static final String TITTLE_TRANSFERS = "Transferencias";

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes homeControllerNodes;


  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(
            homeControllerNodes.getNode(CONNECTION_OPTION_CONTENT),
            homeControllerNodes.getNode(FILES_OPTION_CONTENT),
            homeControllerNodes.getNode(SEARCH_OPTION_CONTENT),
            homeControllerNodes.getNode(TRANSFERS_OPTION_CONTENT)
        ));
  }

  @Override
  public void execute(Event event) {

    final HBox boxClicked = (HBox) event.getSource();
    final Label appTittleText = (Label) homeControllerNodes.getNode(APP_TITTLE_TEXT);

    switch (boxClicked.getId()) {
      case CONNECTION_OPTION_CONTENT -> appTittleText.setText(TITTLE_CONNECTION);
      case FILES_OPTION_CONTENT -> appTittleText.setText(TITTLE_FILES);
      case SEARCH_OPTION_CONTENT -> appTittleText.setText(TITTLE_SEARCH);
      case TRANSFERS_OPTION_CONTENT -> appTittleText.setText(TITTLE_TRANSFERS);
    }
  }
}
