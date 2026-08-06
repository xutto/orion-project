package com.mac.orion.infrastructure.ui.controller;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.domain.model.File;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilesController implements UIController {

  private final ControllerNodes filesControllerNodes;

  @FXML
  private TableView<File> filesTable;
  @FXML
  private TableColumn<File, String> filesNameColumn;
  @FXML
  private TableColumn<File, String> filesPathColumn;
  @FXML
  private TableColumn<File, Long> filesSizeColumn;
  @FXML
  private TableColumn<File, String> filesHashColumn;


  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    filesTable.setColumnResizePolicy(
        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN); // todo control sobre el resize de las columnas
    filesNameColumn.setCellValueFactory(
        f -> new SimpleStringProperty(f.getValue().getNames().stream().findFirst().orElse("")));
    filesPathColumn.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getPath()));
    filesSizeColumn.setCellValueFactory(f -> new SimpleLongProperty(
        FileProcessUtils.convertBytesToMegaBytes(f.getValue().getSize())).asObject());
    filesHashColumn.setCellValueFactory(
        f -> new SimpleStringProperty(f.getValue().getHash().getValue()));

    // TEST TABLE ====================== to delete
//        PodamFactory factory = new PodamFactoryImpl();
//        final ArrayList<File> files = new ArrayList<>();
//        for (int i = 0; i < 40; i++) {
//            final File file = factory.manufacturePojo(File.class);
//            files.add(file);
//        }
//        final ObservableList<File> filesObservable = FXCollections.observableArrayList(files);
//        filesTable.setItems(filesObservable);
    // TEST TABLE ====================== to delete

    // when configured nodes then must add to context
    filesControllerNodes.addNode(filesTable);


  }
}
