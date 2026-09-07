package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.application.out.ProcessRelauncherUseCase;
import com.mac.orion.infrastructure.ui.creation.SettingsAlertsCreator;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_CHANGE_PORT_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_PORT_FIELD;

@Slf4j
@RequiredArgsConstructor
@Component
public class ChangePortCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final NodeConfigUseCase nodeConfigUseCase;
  private final ProcessRelauncherUseCase processRelauncher;
  private final SettingsAlertsCreator alertsCreator;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(settingsControllerNodes.getNode(SETTINGS_CHANGE_PORT_BUTTON)));
  }

  @Override
  public void execute(Event event) {
    final TextField portField = settingsControllerNodes.getNode(SETTINGS_PORT_FIELD, TextField.class);
    final String raw = portField.getText() == null ? "" : portField.getText().trim();

    Integer newPort = null;
    try {
      newPort = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      // not a valid integer, keep null
    }
    if (newPort == null || newPort < 1 || newPort > 65535) {
      alertsCreator.createPortErrorAlert(SettingsAlertsCreator.INVALID_PORT_MESSAGE).showAndWait();
      return;
    }
    final int currentPort = nodeConfigUseCase.getPort();
    if (newPort == currentPort) {
      log.info("Port unchanged ({}), nothing to do", newPort);
      return;
    }

    // Validate the new port is free BEFORE asking for confirmation and before
    // persisting/relaunching: if it is taken, nothing changes (no updatePort, no relaunch).
    if (isPortOccupied(newPort)) {
      log.warn("Port {} is already in use, aborting the change", newPort);
      alertsCreator.createPortOccupiedAlert(newPort).showAndWait();
      return;
    }

    final Alert confirmation = alertsCreator.createPortChangeConfirmationAlert();
    if (confirmation.showAndWait().filter(b -> b == SettingsAlertsCreator.PORT_RESTART).isEmpty()) {
      log.info("Port change cancelled by the user");
      return;
    }

    nodeConfigUseCase.updatePort(newPort);
    if (processRelauncher.relaunch()) {
      log.info("Relaunch started, exiting current instance");
      System.exit(0);
    } else {
      alertsCreator.createPortErrorAlert(
          String.format(SettingsAlertsCreator.PORT_RELAUNCH_FAILED_MESSAGE, newPort)).showAndWait();
    }
  }

  /**
   * Probes whether the given port can be bound locally.
   *
   * @param port the candidate port (1-65535)
   * @return true if the port is already in use, false if it could be reserved (free)
   */
  private boolean isPortOccupied(int port) {
    try (ServerSocket ignored = new ServerSocket(port)) {
      return false; // could bind: port is free
    } catch (IOException e) {
      return true; // BindException (or similar): port is taken
    }
  }
}
