package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.EXPLORE_DOWNLOADS_FOLDER_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.EXPLORE_TEMP_FOLDER_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_APP_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CLOSE_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CONTENT_DIRECTORIES;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_DIRECTORIES_CONTAINER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_MAIN;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_OPTION_CONTENT;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_VIEWER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_SETTINGS_CONTAINER_PANE;
import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_SETTINGS_CONTAINER_PANE_ACTIVE;


@Slf4j
@RequiredArgsConstructor
@Component
public class SwitchSettingsVisibleCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final ControllerNodes homeControllerNodes;
  private final ControllerNodes settingsControllerNodes;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(homeControllerNodes.getNode(SETTINGS_OPTION_CONTENT),
            settingsControllerNodes.getNode(SETTINGS_CLOSE_ICON)));
    compatibilities.put(EventType.KEY_RELEASED,
        List.of(settingsControllerNodes.getNode(SETTINGS_MAIN)));
  }

  @Override
  public void execute(Event event) {

    if (event.getSource() instanceof Node source) {
      if (source.getId().equals(SETTINGS_OPTION_CONTENT)) {

        activeOption(settingsControllerNodes.getNode(SETTINGS_DIRECTORIES_CONTAINER_PANE));
        setVisibleContent(SETTINGS_CONTENT_DIRECTORIES);

        homeControllerNodes.getNode(SETTINGS_APP_CONTENT).setVisible(true);
        settingsControllerNodes.getNode(SETTINGS_MAIN).requestFocus();
      } else if (source.getId().equals(SETTINGS_CLOSE_ICON)) {
        homeControllerNodes.getNode(SETTINGS_APP_CONTENT).setVisible(false);
      }

    } else {
      if (event instanceof KeyEvent keyEvent && hasFocusInside(
          (Parent) homeControllerNodes.getNode(SETTINGS_APP_CONTENT))) {
        if (keyEvent.getCode() == KeyCode.ESCAPE & exclusions()) {
          homeControllerNodes.getNode(SETTINGS_APP_CONTENT).setVisible(false);
          homeControllerNodes.getNode(SETTINGS_OPTION_CONTENT).requestFocus();
        }
      }
    }


  }

  private boolean exclusions() {
    return !settingsControllerNodes.getNode(EXPLORE_DOWNLOADS_FOLDER_BUTTON).isFocused() &&
        !settingsControllerNodes.getNode(EXPLORE_TEMP_FOLDER_BUTTON).isFocused();
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
