package com.mac.orion.infrastructure.ui.command;


import com.mac.orion.application.out.ConnectionInfoProviderUseCase;
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

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_COPY_NODE_ID;

@Slf4j
@RequiredArgsConstructor
@Component
public class CopyNodeIdCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final ConnectionInfoProviderUseCase connectionInfoProvider;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(settingsControllerNodes.getNode(SETTINGS_COPY_NODE_ID)));
  }

  @Override
  public void execute(Event event) {
    final String peerId = connectionInfoProvider.getPeerId();
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    clipboard.clear();
    clipboard.setContent(Map.of(DataFormat.PLAIN_TEXT, peerId));
    log.info("Node ID copied to clipboard: {}", peerId);
  }
}
