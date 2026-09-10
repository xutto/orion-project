package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_BOOTSTRAP;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_COPY_ICON;

/**
 * Copies the PeerId of the clicked bootstrap row to the system clipboard (DECISIÓN: copy = PeerId,
 * coherent with {@link CopyNodeIdCommand}).
 * <p>
 * Per-row command: all row copy icons share the CSS id {@code bootstrap-copy-icon}; the concrete
 * {@link Bootstrap} of the clicked row is carried via the icon's properties
 * ({@code node.getProperties().put(PROPERTIES_BOOTSTRAP, bootstrap)}) — the same mechanism as
 * {@code SearchResultLoader} carries the search id on its table view. {@code configureCompatibility}
 * is (re)invoked by {@code BootstrapListCreator} AFTER the rows exist, since the standard one runs
 * before any dynamic row is built.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BootstrapCopyCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        settingsControllerNodes.getNodes().stream()
            .filter(node -> SETTINGS_BOOTSTRAP_COPY_ICON.equals(node.getId()))
            .toList());
  }

  @Override
  public void execute(Event event) {
    final Node source = (Node) event.getSource();
    final Bootstrap bootstrap = (Bootstrap) source.getProperties().get(PROPERTIES_BOOTSTRAP);
    if (bootstrap == null) {
      log.warn("Copy clicked on a row without a bootstrap payload; nothing to copy");
      return;
    }
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    clipboard.clear();
    clipboard.setContent(Map.of(DataFormat.PLAIN_TEXT, bootstrap.id()));
    log.info("Bootstrap PeerId copied to clipboard: {} ({}:{})", bootstrap.id(), bootstrap.ip(),
        bootstrap.port());
  }
}
