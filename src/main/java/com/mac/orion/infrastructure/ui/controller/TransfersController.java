package com.mac.orion.infrastructure.ui.controller;

import com.mac.orion.domain.model.Transfer;
import com.mac.orion.infrastructure.ui.creation.FragmentProgressBar;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransfersController implements UIController {

  private final ControllerNodes transfersControllerNodes;

  @FXML
  private TableView<Transfer> transfersTable;
  @FXML
  private TableColumn<Transfer, String> transfersNameColumn;
  @FXML
  private TableColumn<Transfer, String> transfersCompletedColumn;
  @FXML
  private TableColumn<Transfer, String> transfersSpeedColumn;
  @FXML
  private TableColumn<Transfer, String> transfersNodesColumn;
  @FXML
  private TableColumn<Transfer, Transfer> transfersProgressColumn;

  @Override
  public void initialize(URL location, ResourceBundle resources) {

    transfersTable.setColumnResizePolicy(
        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    transfersNameColumn.setCellValueFactory(
        f -> new SimpleStringProperty(f.getValue().getNames().stream().findFirst().orElse("")));
    transfersCompletedColumn.setCellValueFactory(
        f -> new SimpleStringProperty(
            f.getValue().getCompleted() + " / " + f.getValue().getSize()));
    transfersSpeedColumn.setCellValueFactory(
        f -> new SimpleStringProperty(f.getValue().getSpeed() + " B/s"));
    transfersNodesColumn.setCellValueFactory(
        f -> new SimpleStringProperty(String.valueOf(f.getValue().getPeers())));

    transfersProgressColumn.setCellValueFactory(cd ->
        new ReadOnlyObjectWrapper<>(cd.getValue()));

    transfersProgressColumn.setCellFactory(col -> new TableCell<>() {
      // Creamos una instancia del componente para esta celda específica
      private final FragmentProgressBar fragmentProgressBar = new FragmentProgressBar().create();

      @Override
      protected void updateItem(Transfer transfer, boolean empty) {
        super.updateItem(transfer, empty);
        if (empty || transfer == null) {
          setGraphic(null);
        } else {
          // Pasamos los datos que ya tienes en el modelo Transfer
          fragmentProgressBar.update(transfer);
          setGraphic(fragmentProgressBar); // Metemos el canvas dentro de la celda
        }
      }
    });

    // TEST TABLE ====================== to delete
//    PodamFactory factory = new PodamFactoryImpl();
//    final ArrayList<Transfer> transfers = new ArrayList<>();
//    for (int i = 0; i < 10; i++) {
//      final Transfer transfer = factory.manufacturePojo(Transfer.class);
//      List<Integer> transferStatus = List.of(0,1, 2, 3, 5, 6, 8, 9,48,15,45, 66,99,80,81,82);
//      final Transfer mod = transfer.toBuilder()
//          .transferStatus(transferStatus)
//          .totalFragments(100)
//          .build();
//      transfers.add(mod);
//    }
//    log.info("Transfers: {}", transfers);
//    final ObservableList<Transfer> filesObservable = FXCollections.observableArrayList(transfers);
//    transfersTable.setItems(filesObservable);
    // TEST TABLE ====================== to delete

    transfersControllerNodes.addNode(transfersTable);
  }
}
