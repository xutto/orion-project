package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.infrastructure.ui.connection.ConnectionInfoProvider;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_LABEL_IP;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_LABEL_NODE_ID;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_LABEL_PORT;

@Slf4j
@RequiredArgsConstructor
@Component
public class UpdateConnectionInfoCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final ConnectionInfoProvider connectionInfoProvider;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.ON_BOOT, List.of());
  }

  @Override
  public void execute() {
    Platform.runLater(() -> {
      try {
        final Label ipLabel = settingsControllerNodes.getNode(SETTINGS_LABEL_IP, Label.class);
        final Label portLabel = settingsControllerNodes.getNode(SETTINGS_LABEL_PORT, Label.class);
        final Label nodeIdLabel = settingsControllerNodes.getNode(SETTINGS_LABEL_NODE_ID, Label.class);

        ipLabel.setText(connectionInfoProvider.getLocalIp());
        portLabel.setText(connectionInfoProvider.getPort());
        nodeIdLabel.setText(connectionInfoProvider.getPeerId());

        // async: resolves the public IP (https://api.ipify.org) and updates the IP label when ready
        connectionInfoProvider.refreshPublicIp();

        log.info("Connection info updated: {}:{}, peer: {}",
            ipLabel.getText(), portLabel.getText(), nodeIdLabel.getText());
      } catch (Exception e) {
        log.error("Failed to update the connection info panel", e);
      }
    });
  }
}
