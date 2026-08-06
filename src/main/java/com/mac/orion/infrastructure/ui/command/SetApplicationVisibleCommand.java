package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.CONNECTION_APP_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.CONNECTION_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.FILES_APP_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.FILES_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.MAIN_MENU;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_APP_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SEARCH_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.STACK_APP_CONTAINER;
import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_MENU_OPTION_CONTENT_SELECTED;
import static com.mac.orion.domain.share.NodesIdentifierConstants.TITTLE_TRADEMARK;
import static com.mac.orion.domain.share.NodesIdentifierConstants.TRANSFERS_APP_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.TRANSFERS_OPTION_CONTENT;

import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SetApplicationVisibleCommand implements Command<Event> {

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

    // get clicked hBox
    final HBox boxClicked = (HBox) event.getSource();

    // all apps set not visible
    homeControllerNodes.getNode(STACK_APP_CONTAINER, StackPane.class).getChildren()
        .forEach(n -> n.setVisible(false));

    final String optionContent = boxClicked.getId();

    switch (optionContent) {
      case CONNECTION_OPTION_CONTENT -> setVisible(CONNECTION_APP_CONTENT);
      case FILES_OPTION_CONTENT -> setVisible(FILES_APP_CONTENT);
      case SEARCH_OPTION_CONTENT -> setVisible(SEARCH_APP_CONTENT);
      case TRANSFERS_OPTION_CONTENT -> setVisible(TRANSFERS_APP_CONTENT);
    }

    homeControllerNodes.getNode(optionContent).getStyleClass()
        .add(STYLE_CLASS_MENU_OPTION_CONTENT_SELECTED);
  }

  private void setVisible(String appContent) {
    homeControllerNodes.getNode(appContent).setVisible(true);

    homeControllerNodes.getNode(MAIN_MENU, VBox.class).getChildren().stream()
        .filter(n -> !n.getId().equals(TITTLE_TRADEMARK)) //exclude trademark node
        .forEach(n -> n.getStyleClass().remove(STYLE_CLASS_MENU_OPTION_CONTENT_SELECTED));
  }
}
