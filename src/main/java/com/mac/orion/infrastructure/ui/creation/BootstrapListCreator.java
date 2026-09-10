package com.mac.orion.infrastructure.ui.creation;

import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.infrastructure.ui.command.BootstrapCopyCommand;
import com.mac.orion.infrastructure.ui.command.BootstrapDeleteCommand;
import com.mac.orion.infrastructure.ui.events.EventsAssembler;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_BOOTSTRAP;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_COPY_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_DELETE_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_LIST_CONTAINER;

/**
 * Builds the live bootstrap list in Settings from the DB (the single source of truth) and (re)wires
 * the per-row commands. The 5 static example rows in {@code settings.fxml} are replaced by real ones.
 * <p>
 * Per-row command wiring: the row copy/delete icons share the CSS ids {@code bootstrap-copy-icon} /
 * {@code bootstrap-delete-icon}; the concrete {@link Bootstrap} of each row is carried on the icon's
 * properties ({@code PROPERTIES_BOOTSTRAP}). Because the standard {@code configureCompatibility()}
 * pass runs BEFORE any dynamic row exists (see {@code DesktopApplicationLoader}), this creator
 * re-invokes {@code configureCompatibility()} + {@code configureCommandEvents(...)} AFTER building the
 * rows — the same re-wiring {@code SearchResultLoader} performs for its dynamic table view.
 */
@Slf4j
@RequiredArgsConstructor
public class BootstrapListCreator implements Creator {

  private static final String COPY_SVG =
      "M192 0c-35.3 0-64 28.7-64 64l0 256c0 35.3 28.7 64 64 64l192 0c35.3 0 64-28.7 64-64l0-200.6"
          + "c0-17.4-7.1-34.1-19.7-46.2L370.6 17.8C358.7 6.4 342.8 0 326.3 0L192 0zM64 128c-35.3 0"
          + "-64 28.7-64 64L0 448c0 35.3 28.7 64 64 64l192 0c35.3 0 64-28.7 64-64l0-16-64 0 0 16"
          + "-192 0 0-256 16 0 0-64-16 0z";
  private static final String DELETE_SVG =
      "M136.7 5.9C141.1-7.2 153.3-16 167.1-16l113.9 0c13.8 0 26 8.8 30.4 21.9L320 32 416 32c17.7 0"
          + " 32 14.3 32 32s-14.3 32-32 32L32 96C14.3 96 0 81.7 0 64S14.3 32 32 32l96 0 8.7-26.1z"
          + "M32 144l384 0 0 304c0 35.3-28.7 64-64 64L96 512c-35.3 0-64-28.7-64-64l0-304zm88 64"
          + "c-13.3 0-24 10.7-24 24l0 192c0 13.3 10.7 24 24 24s24-10.7 24-24l0-192c0-13.3-10.7-24"
          + "-24-24zm104 0c-13.3 0-24 10.7-24 24l0 192c0 13.3 10.7 24 24 24s24-10.7 24-24l0-192"
          + "c0-13.3-10.7-24-24-24zm104 0c-13.3 0-24 10.7-24 24l0 192c0 13.3 10.7 24 24 24s24-10.7"
          + " 24-24l0-192c0-13.3-10.7-24-24-24z";

  private final ControllerNodes settingsControllerNodes;
  private final BootstrapUseCase bootstrapUseCase;
  private final BootstrapCopyCommand bootstrapCopyCommand;
  private final BootstrapDeleteCommand bootstrapDeleteCommand;
  private final EventsAssembler eventsAssembler;

  @Override
  public void init() {
    // Rows are rendered from SettingsController.initialize() (when the panel loads); nothing to prebuild.
    log.debug("BootstrapListCreator initialized");
  }

  /**
   * Rebuilds the whole list from the DB (idempotent single source of truth) and (re)wires the
   * per-row copy/delete commands. Called on startup (Settings panel) and after each add/delete.
   */
  public void refresh() {
    final VBox container = settingsControllerNodes.getNode(SETTINGS_BOOTSTRAP_LIST_CONTAINER, VBox.class);
    // Drop the previous render's per-row icons so configureCompatibility only sees the new nodes.
    settingsControllerNodes.getNodes().removeIf(
        node -> SETTINGS_BOOTSTRAP_COPY_ICON.equals(node.getId())
            || SETTINGS_BOOTSTRAP_DELETE_ICON.equals(node.getId()));
    container.getChildren().clear();
    bootstrapUseCase.findAll().forEach(bootstrap -> addRow(container, bootstrap));

    // (Re)wire the per-row commands NOW that the dynamic rows exist (standard wiring ran before them).
    bootstrapCopyCommand.configureCompatibility();
    bootstrapDeleteCommand.configureCompatibility();
    eventsAssembler.configureCommandEvents(
        List.of(bootstrapCopyCommand, bootstrapDeleteCommand));
    log.info("Bootstrap list rendered: {} row(s)", container.getChildren().size());
  }

  private void addRow(VBox container, Bootstrap bootstrap) {
    final StackPane copyIcon = buildIcon(SETTINGS_BOOTSTRAP_COPY_ICON, "copy-icon", COPY_SVG);
    final StackPane deleteIcon = buildIcon(SETTINGS_BOOTSTRAP_DELETE_ICON, "delete-icon", DELETE_SVG);
    // Per-row payload: the dispatcher routes by (shared) CSS id, so the concrete row travels here.
    copyIcon.getProperties().put(PROPERTIES_BOOTSTRAP, bootstrap);
    deleteIcon.getProperties().put(PROPERTIES_BOOTSTRAP, bootstrap);

    final HBox row = new HBox();
    row.getChildren().addAll(
        buildField(bootstrap.ip()),
        buildField(bootstrap.port()),
        buildField(bootstrap.id()),
        copyIcon,
        deleteIcon);

    container.getChildren().add(row);
    settingsControllerNodes.addNode(copyIcon).addNode(deleteIcon);
  }

  private StackPane buildField(String text) {
    final Label label = new Label(text);
    label.setTextFill(Color.WHITE);
    final StackPane field = new StackPane(label);
    field.setAlignment(Pos.CENTER_LEFT);
    field.setMinHeight(30);
    field.setPrefHeight(30);
    field.getStyleClass().add("settings-content-body-result-label");
    return field;
  }

  private StackPane buildIcon(String id, String styleClass, String svg) {
    final SVGPath icon = new SVGPath();
    icon.setContent(svg);
    icon.getStyleClass().add("icon");
    final StackPane pane = new StackPane(icon);
    pane.setId(id);
    pane.setMinSize(32, 45);
    pane.setPrefSize(32, 45);
    pane.setMaxSize(32, 45);
    pane.getStyleClass().add(styleClass);
    return pane;
  }
}
