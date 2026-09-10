package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.infrastructure.ui.creation.BootstrapListCreator;
import com.mac.orion.infrastructure.ui.creation.SettingsAlertsCreator;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.PROPERTIES_BOOTSTRAP;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_DELETE_ICON;

/**
 * Deletes a bootstrap from the DB only (DECISIÓN-05): it does NOT emit {@code REMOVE} on the routing
 * table, so the in-memory/ephemeral peers are untouched — the bootstrap simply stops being seeded at
 * the next startup. Requires an explicit confirm/cancel dialog (DECISIÓN-05 matiz).
 * <p>
 * Per-row command: like {@link BootstrapCopyCommand}, the concrete {@link Bootstrap} of the clicked
 * row is carried via the icon's properties. {@code BootstrapListCreator} is injected as an
 * {@link ObjectProvider} (lazy) to break the creator → delete-command → creator cycle; it is only
 * resolved AFTER the deletion, to re-render the list.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BootstrapDeleteCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final BootstrapUseCase bootstrapUseCase;
  private final SettingsAlertsCreator alertsCreator;
  private final ObjectProvider<BootstrapListCreator> bootstrapListCreatorProvider;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        settingsControllerNodes.getNodes().stream()
            .filter(node -> SETTINGS_BOOTSTRAP_DELETE_ICON.equals(node.getId()))
            .toList());
  }

  @Override
  public void execute(Event event) {
    final Node source = (Node) event.getSource();
    final Bootstrap bootstrap = (Bootstrap) source.getProperties().get(PROPERTIES_BOOTSTRAP);
    if (bootstrap == null) {
      log.warn("Delete clicked on a row without a bootstrap payload; nothing to delete");
      return;
    }

    if (alertsCreator.createDeleteBootstrapConfirmationAlert().showAndWait()
        .filter(b -> b == SettingsAlertsCreator.DELETE_CONFIRM).isEmpty()) {
      log.info("Bootstrap deletion cancelled by the user: {}:{} (id={})", bootstrap.ip(),
          bootstrap.port(), bootstrap.id());
      return;
    }

    bootstrapUseCase.delete(bootstrap); // DB only (DECISIÓN-05): no routing-table REMOVE
    log.info("Bootstrap deleted: {}:{} (id={})", bootstrap.ip(), bootstrap.port(), bootstrap.id());
    bootstrapListCreatorProvider.getObject().refresh(); // list reflects the DB again
  }
}
