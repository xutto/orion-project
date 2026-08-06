package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.TRANSFERS_TABLE;

import com.mac.orion.application.service.publisher.DownloadingPublisherService;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Transfer;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class DownloadsWatcherCommand<T extends Event> implements Command<T> {

  private final Map<Hash, Integer> indexController = new HashMap<>();

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final ControllerNodes transfersControllerNodes;
  private final DownloadingPublisherService downloadingPublisherService;

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

    downloadingPublisherService.subscribe(OperationsType.DOWNLOADING, transfer -> {
      Platform.runLater(() -> {
        final TableView<Transfer> tableView = transfersControllerNodes.getNode(TRANSFERS_TABLE, TableView.class);

        if (!indexController.containsKey(transfer.getHash())) {
          // Si es nuevo, lo añadimos a la tabla de descargas
          int index = tableView.getItems().size();
          tableView.getItems().add(transfer);
          indexController.put(transfer.getHash(), index);
        } else {
          // Si ya existe, actualizamos su progreso/estado
          int index = indexController.get(transfer.getHash());
          tableView.getItems().set(index, transfer);
        }
      });

    });

    downloadingPublisherService.subscribe(OperationsType.REMOVE, transfer -> Platform.runLater(() -> {
      final TableView<Transfer> tableView = transfersControllerNodes.getNode(TRANSFERS_TABLE, TableView.class);
      tableView.getItems().removeIf(t -> t.getHash().equals(transfer.getHash()));
    }));


  }
}
