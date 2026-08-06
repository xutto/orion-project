package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.infrastructure.ui.creation.TooltipsCreator;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.stage.PopupWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_NODES_HELP_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CONNECTION_DATA_HELP_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_LIMIT_HELP_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_MAIN;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_TOOLTIP_BOOTSTRAP;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_TOOLTIP_CONNECTION;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_TOOLTIP_LIMIT_K;

@Slf4j
@RequiredArgsConstructor
@Component
public class ToggleHelpInfoOnSettingsCommand implements Command<Event> {

  private static final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final TooltipsCreator tooltipsCreator;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED, List.of(
        settingsControllerNodes.getNode(SETTINGS_CONNECTION_DATA_HELP_ICON),
        settingsControllerNodes.getNode(SETTINGS_BOOTSTRAP_NODES_HELP_ICON),
        settingsControllerNodes.getNode(SETTINGS_LIMIT_HELP_ICON),
        settingsControllerNodes.getNode(SETTINGS_MAIN)
    ));
  }

  /**
   * Executes the specified event by handling tooltips display logic for settings-related help icons.
   * Depending on the event source, this method toggles the visibility of corresponding tooltips and
   * hides those that are not relevant to the current event.
   *
   * @param event the JavaFX event to be executed, used for determining the source node and its ID
   */
  @Override
  public void execute(Event event) {

    // hide all tooltips
    final List<Tooltip> settingsTooltipHelpers = tooltipsCreator.getSettingsTooltipHelpers();

    if (event.getSource() instanceof Node source) {

      if (!settingsTooltipHelpers.stream().map(PopupControl::getId).toList().contains(source.getId())){
        settingsTooltipHelpers.forEach(PopupWindow::hide);
      }

      switch (source.getId()) {
        case SETTINGS_CONNECTION_DATA_HELP_ICON ->
            settingsTooltipHelpers.stream().filter(t -> t.getId().equals(SETTINGS_TOOLTIP_CONNECTION)).findFirst()
                .ifPresent(t -> Platform.runLater(() -> toggleTooltip(source, t, event)));
        case SETTINGS_BOOTSTRAP_NODES_HELP_ICON ->
            settingsTooltipHelpers.stream().filter(t -> t.getId().equals(SETTINGS_TOOLTIP_BOOTSTRAP)).findFirst()
                .ifPresent(t -> Platform.runLater(() -> toggleTooltip(source, t, event)));
        case SETTINGS_LIMIT_HELP_ICON ->
            settingsTooltipHelpers.stream().filter(t -> t.getId().equals(SETTINGS_TOOLTIP_LIMIT_K)).findFirst()
                .ifPresent(t -> Platform.runLater(() -> toggleTooltip(source, t, event)));
      }
    }
  }


  private void toggleTooltip(Node owner, Tooltip tooltip, Event event) {
    if (event instanceof MouseEvent mouseEvent) {
      if (tooltip.isShowing()) {
        tooltip.hide();
      } else {
        tooltip.show(owner, mouseEvent.getScreenX(), mouseEvent.getScreenY() + 15);
      }
    }
  }

  private boolean isDescendantOf(Node node, Node ancestor) {
    if (node == null || node.equals(ancestor)) return false;
    Node parent = node.getParent();
    while (parent != null) {
      if (parent.equals(ancestor)) return true;
      parent = parent.getParent();
    }
    return false;
  }


}
