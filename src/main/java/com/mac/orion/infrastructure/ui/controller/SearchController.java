package com.mac.orion.infrastructure.ui.controller;

import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SearchController implements UIController {

  private final ControllerNodes searchControllerNodes;

  @FXML
  private TextField searchBar;
  @FXML
  private Button searchButton;
  @FXML
  private TabPane searchTabs;
  @FXML
  private Button downloadButton;
  @FXML
  private HBox downloadContainer;


  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    // TEST TABLE ====================== to delete
//        PodamFactory factory = new PodamFactoryImpl();
//        final ArrayList<File> files = new ArrayList<>();
//        for (int i = 0; i < 40; i++) {
//            final File file = factory.manufacturePojo(File.class);
//            files.add(file);
//        }
//        final ObservableList<File> filesObservable = FXCollections.observableArrayList(files);
//    searchTable.setItems(filesObservable);
    // TEST TABLE ====================== to delete

    searchControllerNodes
        .addNode(searchBar)
        .addNode(searchButton)
        .addNode(searchTabs)
        .addNode(downloadButton)
        .addNode(downloadContainer);

  }
}
