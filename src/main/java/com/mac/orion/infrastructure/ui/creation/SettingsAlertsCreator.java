package com.mac.orion.infrastructure.ui.creation;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the settings warning dialogs (responsibility of this package: creating new "nodes").
 * Same dark theme as dialog.css and undecorated: no OS title/close bar (request [001],
 * point 3 — "las quiero sin 'ventana'"). Per DECISIÓN-01 (option 2, resolved 2026-09-06)
 * the port dialogs (ChangePortCommand) reuse this creator too: it owns their factories and
 * texts, the command only orchestrates.
 */
@Slf4j
public class SettingsAlertsCreator implements Creator {

  public static final String LIMIT_K_ALERT_TITLE = "LimitK";
  public static final String INVALID_LIMIT_K_MESSAGE = "Invalid LimitK. Enter a value between 1 and 100.";

  public static final String BOOTSTRAP_ALERT_TITLE = "Bootstrap";
  public static final String INVALID_BOOTSTRAP_MESSAGE =
      "Invalid bootstrap. Enter a valid IPv4 address, a port between 1 and 65535 and a valid peer ID.";

  public static final String CHANGE_PORT_TITLE = "Change port";
  public static final String PORT_CHANGE_CONFIRMATION_HEADER_TEXT = "Changing the port will RESTART the connection.";
  public static final String PORT_CHANGE_CONFIRMATION_TEXT = "The node will get a NEW peer ID and existing peers will lose their routing data about this node.\n"
      + "The bootstrap node list is NOT affected.\n\nRestart the connection now?";
  public static final String INVALID_PORT_MESSAGE = "Invalid port. Enter a value between 1 and 65535.";
  public static final String PORT_OCCUPIED_MESSAGE = "Port %d is already in use on this machine. Choose a free port.";
  public static final String PORT_RELAUNCH_FAILED_MESSAGE = "The application could not be relaunched. The port %d"
      + " was saved: restart the application manually to apply it.";

  /**
   * Custom "Restart" button (ButtonType.RESTART does not exist in standard JavaFX).
   * Owned here so ChangePortCommand compares against the same instance it is shown with.
   */
  public static final ButtonType PORT_RESTART = new ButtonType("Restart");

  private static final String DIALOG_STYLESHEET =
      SettingsAlertsCreator.class.getResource("/ui/style/dialog.css").toExternalForm();

  @Override
  public void init() {
    // Alerts are one-shot and built on demand; nothing to prebuild.
    log.debug("SettingsAlertsCreator initialized");
  }

  /**
   * Warning for an invalid LimitK value (out of 1..100 or not numeric).
   * Single OK button; also closable with ESC (native Alert behavior).
   */
  public Alert createInvalidLimitKAlert() {
    final Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(LIMIT_K_ALERT_TITLE);
    alert.setHeaderText(null);
    alert.setContentText(INVALID_LIMIT_K_MESSAGE);
    applyDialogStyle(alert);
    return alert;
  }

  /**
   * Warning for an invalid bootstrap contact (bad IPv4, out-of-range port or not a valid peer ID).
   * Single OK button; also closable with ESC (native Alert behavior).
   */
  public Alert createInvalidBootstrapAlert() {
    final Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(BOOTSTRAP_ALERT_TITLE);
    alert.setHeaderText(null);
    alert.setContentText(INVALID_BOOTSTRAP_MESSAGE);
    applyDialogStyle(alert);
    return alert;
  }

  /**
   * Error for the port change flow (generic message, e.g. invalid port).
   * Single OK button; also closable with ESC (native Alert behavior).
   */
  public Alert createPortErrorAlert(String message) {
    final Alert error = new Alert(Alert.AlertType.ERROR);
    error.setTitle(CHANGE_PORT_TITLE);
    error.setHeaderText(null);
    error.setContentText(message);
    applyDialogStyle(error);
    return error;
  }

  /**
   * Error for a port that is already in use on this machine.
   * Single OK button; also closable with ESC (native Alert behavior).
   */
  public Alert createPortOccupiedAlert(int port) {
    return createPortErrorAlert(String.format(PORT_OCCUPIED_MESSAGE, port));
  }

  /**
   * Confirmation before persisting a new port and relaunching.
   * Buttons: {@link #PORT_RESTART} and CANCEL; compare the result with
   * {@code showAndWait().filter(b -> b == PORT_RESTART)}.
   */
  public Alert createPortChangeConfirmationAlert() {
    final Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setTitle(CHANGE_PORT_TITLE);
    confirmation.setHeaderText(PORT_CHANGE_CONFIRMATION_HEADER_TEXT);
    confirmation.setContentText(PORT_CHANGE_CONFIRMATION_TEXT);
    confirmation.getButtonTypes().setAll(PORT_RESTART, ButtonType.CANCEL);
    applyDialogStyle(confirmation);
    return confirmation;
  }

  /**
   * Reusable dialog styling (theme + "no window"): dark theme from dialog.css and an
   * undecorated stage (no OS title/close bar). Applied by every factory above.
   * <p>
   * Must be applied BEFORE the dialog is shown: initStyle throws IllegalStateException
   * once the dialog has ever been visible (JavaFX 23 API contract).
   */
  public void applyDialogStyle(Alert alert) {
    alert.getDialogPane().getStylesheets().add(DIALOG_STYLESHEET);
    alert.initStyle(StageStyle.UNDECORATED);
  }
}
