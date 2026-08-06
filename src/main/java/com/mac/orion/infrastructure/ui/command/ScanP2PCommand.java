package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.PEERS_TABLE;

import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScanP2PCommand<T extends Event> implements Command<T> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final ControllerNodes connectionControllerNodes;
  private final RoutingTable routingTable;

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
    routingTable.subscribe(OperationsType.SAVE, p -> {
      log.debug("Peer found: {}", p);
      Platform.runLater(() -> {
        final TableView<Peer> tableView = connectionControllerNodes.getNode(PEERS_TABLE,
            TableView.class);
        final Optional<Peer> existingPeer = tableView.getItems().stream()
            .filter(p1 -> p1.getId().equals(p.getId())).findFirst();
        if (existingPeer.isPresent()) {

          final int index = tableView.getItems().indexOf(existingPeer.get());
          tableView.getItems().set(index, p);

        }else {
          tableView.getItems().add(p);
        }

      });
    });

    routingTable.subscribe(OperationsType.REMOVE, p -> {
      log.info("Remove peer: {}", p);
      Platform.runLater(() -> {
        final TableView<Peer> tableView = connectionControllerNodes.getNode(PEERS_TABLE,
            TableView.class);
        tableView.getItems().removeIf(p1 -> p1.getId().equals(p.getId()));
      });
    });

  }
}
