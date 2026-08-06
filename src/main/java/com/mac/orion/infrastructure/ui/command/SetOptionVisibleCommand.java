package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CONNECTION_CONTAINER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CONTENT_CONNECTION;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CONTENT_DIRECTORIES;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_DIRECTORIES_CONTAINER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_VIEWER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_SETTINGS_CONTAINER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_SETTINGS_CONTAINER_PANE_ACTIVE;


@Slf4j
@RequiredArgsConstructor
@Component
public class SetOptionVisibleCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final ControllerNodes settingsControllerNodes;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(
            settingsControllerNodes.getNode(SETTINGS_DIRECTORIES_CONTAINER_PANE),
            settingsControllerNodes.getNode(SETTINGS_CONNECTION_CONTAINER_PANE)
        ));
    compatibilities.put(EventType.ON_BOOT, List.of());
  }

  @Override
  public void execute(Event event) {
    final Node sourceOption = (Node) event.getSource();

    activeOption(sourceOption);

    final String sourceOptionId = sourceOption.getId();

    switch (sourceOptionId) {
      case SETTINGS_DIRECTORIES_CONTAINER_PANE -> setVisibleContent(SETTINGS_CONTENT_DIRECTORIES);
      case SETTINGS_CONNECTION_CONTAINER_PANE -> setVisibleContent(SETTINGS_CONTENT_CONNECTION);
    }

  }

  @Override
  public void execute() {
    // initial option to select
    activeOption(settingsControllerNodes.getNode(SETTINGS_DIRECTORIES_CONTAINER_PANE));

    // initial pane to visible
    setVisibleContent(SETTINGS_CONTENT_DIRECTORIES);
  }

  private void setVisibleContent(String content) {

    settingsControllerNodes.getNode(SETTINGS_VIEWER_PANE, StackPane.class)
        .getChildren()
        .forEach(n -> n.setVisible(false));

    final Node directoriesContent = settingsControllerNodes.getNode(content);
    directoriesContent.setVisible(true);

  }

  private void activeOption(Node sourceOption) {
    settingsControllerNodes.getNodes().stream()
        .filter(n -> n.getStyleClass().contains(STYLE_CLASS_SETTINGS_CONTAINER_PANE))
        .forEach(n -> n.getStyleClass().remove(STYLE_CLASS_SETTINGS_CONTAINER_PANE_ACTIVE));

    sourceOption.getStyleClass().add(STYLE_CLASS_SETTINGS_CONTAINER_PANE_ACTIVE);
  }
}
