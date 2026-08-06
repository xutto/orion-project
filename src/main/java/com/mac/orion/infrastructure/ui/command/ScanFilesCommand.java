package com.mac.orion.infrastructure.ui.command;

import static com.mac.orion.domain.share.NodesIdentifierConstants.FILES_TABLE;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.application.service.MaintenanceFilesService;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
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
public class ScanFilesCommand<T extends Event> implements Command<T> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final Map<Hash, Integer> indexController = new HashMap<>();
  //  private final ScanFilesUseCase scanFilesUseCase;
  private final MaintenanceFilesService maintenanceFilesService; // todo proceso de mantenimiento disparado tambien al arranque¿
  private final Publisher<File> filePublisherService;
  private final ControllerNodes filesControllerNodes;

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
    log.info("Executing ScanFilesCommand");

    //todo [CONFIGURATION] la configuracion de los directorios debe estar en el configuration external.
    // todo CREAR el folder o comprobar que existe y si no existe crearlo.

    filePublisherService.subscribe(OperationsType.SAVE, f ->
        Platform.runLater(() -> {
          log.debug("THE FILE TO SAVE {}", f);
          TableView<File> tableView = filesControllerNodes.getNode(FILES_TABLE, TableView.class);
          if (!indexController.containsKey(f.getHash())) {
            // add if not exists
            final int index = tableView.getItems().size();
            tableView.getItems().add(index, f);
            indexController.put(f.getHash(), index);
          }
        })
    );

    filePublisherService.subscribe(OperationsType.REMOVE, f ->
        Platform.runLater(() -> {
          log.info("THE FILE TO REMOVE {}", f);
          TableView<File> tableView = filesControllerNodes.getNode(FILES_TABLE, TableView.class);
          if (indexController.containsKey(f.getHash())) {
            final int index = indexController.get(f.getHash());
            tableView.getItems().remove(index);
          }
        })
    );


  }
}
