package com.mac.orion.infrastructure.ui.controller;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;


@Configuration
@Scope("prototype") // todo importante
@RequiredArgsConstructor
public class SearchResultController implements UIController {

  private final ControllerNodes searchResultControllerNodes;

  @FXML
  private TableView<File> searchTable;
  @FXML
  private TableColumn<File, String> searchNameColumn;
  @FXML
  private TableColumn<File, Long> searchSizeColumn;
  @FXML
  private TableColumn<File, String> searchHashColumn;
  @FXML
  private TableColumn<File, Integer> searchFilePeersColumn;


  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    searchTable.setColumnResizePolicy(
        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN); // todo control sobre el resize de las columnas

    // Selección por fila (no por celda)
    searchTable.getSelectionModel().setCellSelectionEnabled(false);
    searchTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    searchNameColumn.setCellValueFactory(
        f -> new SimpleStringProperty(f.getValue().getNames().stream().findFirst().orElse("")));
    searchSizeColumn.setCellValueFactory(f -> new SimpleLongProperty(
        FileProcessUtils.convertBytesToMegaBytes(f.getValue().getSize())).asObject());
    searchHashColumn.setCellValueFactory(
        f -> new SimpleStringProperty(f.getValue().getHash().getValue()));
    searchFilePeersColumn.setCellValueFactory(
        f -> new SimpleIntegerProperty(f.getValue().getPeers().size())
            .asObject());

    searchResultControllerNodes.addNode(searchTable);

  }

  /*


.setCellFactory(col -> new TableCell<>() {
        private final Button btn = new Button("Descargar");
        { btn.getStyleClass().add("row-action-download"); } // ← tu marca
        @Override protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : btn);
            setText(null);
        }
    });


  */
}
