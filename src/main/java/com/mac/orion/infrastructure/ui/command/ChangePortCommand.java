package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.application.out.ProcessRelauncherUseCase;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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

  public static final String CHANGE_PORT_TITTLE = "Change port";
  public static final String PORT_CHANGE_CONFIRMATION_HEADER_TEXT = "Changing the port will RESTART the connection.";
  public static final String PORT_CHANGE_CONFIRMATION_TEXT = "The node will get a NEW peer ID and existing peers will lose their routing data about this node.\n"
      + "The bootstrap node list is NOT affected.\n\nRestart the connection now?";
  public static final String INVALID_PORT_MESSAGE = "Invalid port. Enter a value between 1 and 65535.";
  public static final String PORT_OCCUPIED_MESSAGE = "Port %d is already in use on this machine. Choose a free port.";
  private static final String DIALOG_STYLESHEET =
      ChangePortCommand.class.getResource("/ui/style/dialog.css").toExternalForm();
  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final NodeConfigUseCase nodeConfigUseCase;
  private final ProcessRelauncherUseCase processRelauncher;

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
      showError(INVALID_PORT_MESSAGE);
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
      showError(String.format(PORT_OCCUPIED_MESSAGE, newPort));
      return;
    }

    // ButtonType.RESTART does not exist in standard JavaFX: custom button with text "Restart"
    final ButtonType restart = new ButtonType("Restart");
    final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setTitle(CHANGE_PORT_TITTLE);
    confirmation.setHeaderText(PORT_CHANGE_CONFIRMATION_HEADER_TEXT);
    confirmation.setContentText(PORT_CHANGE_CONFIRMATION_TEXT);
    confirmation.getButtonTypes().setAll(restart, ButtonType.CANCEL);
    applyDialogTheme(confirmation);

    if (confirmation.showAndWait().filter(b -> b == restart).isEmpty()) {
      log.info("Port change cancelled by the user");
      return;
    }

    nodeConfigUseCase.updatePort(newPort);
    if (processRelauncher.relaunch()) {
      log.info("Relaunch started, exiting current instance");
      System.exit(0);
    } else {
      showError("The application could not be relaunched. The port " + newPort
          + " was saved: restart the application manually to apply it.");
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

  /** Applies the app dark theme so the alert matches the rest of the UI (settings.css palette). */
  private void applyDialogTheme(Alert alert) {
    alert.getDialogPane().getStylesheets().add(DIALOG_STYLESHEET);
  }

  private void showError(String message) {
    final Alert error = new Alert(Alert.AlertType.ERROR);
    error.setTitle(CHANGE_PORT_TITTLE);
    error.setHeaderText(null);
    error.setContentText(message);
    applyDialogTheme(error);
    error.showAndWait();
  }
}
