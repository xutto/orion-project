package com.mac.orion.infrastructure.ui.controller;

import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_MENU_OPTION_CONTENT_SELECTED;
import static com.mac.orion.infrastructure.ui.command.ChangeTitleNamesCommand.TITTLE_TRANSFERS;

import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
//@Getter
public class HomeController implements UIController {

  private final ControllerNodes homeControllerNodes;

  @FXML
  private HBox filesAppContent;
  @FXML
  private HBox connectionAppContent;
  @FXML
  private HBox filesOptionContent;
  @FXML
  private StackPane stackAppContainer;
  @FXML
  private HBox connectionOptionContent;
  @FXML
  private VBox mainMenu;
  @FXML
  private HBox searchOptionContent;
  @FXML
  private HBox searchAppContent;
  @FXML
  private HBox transfersOptionContent;
  @FXML
  private HBox transfersAppContent;
  @FXML
  private Label appTittleText;
  @FXML
  private Node settingsAppContent;
  @FXML
  private HBox settingsOptionContent;

  //    private final ConfigurableApplicationContext context;
//    private final HomeEvents homeEvents;


  public void initialize(URL url, ResourceBundle resourceBundle) {
    stackAppContainer.getChildren().forEach(n -> n.setVisible(false)); // todo controlar en alguna capa la visibilidad inicial de las aplicaciones principales
    transfersAppContent.setVisible(true);
    transfersOptionContent.getStyleClass().add(STYLE_CLASS_MENU_OPTION_CONTENT_SELECTED); // todo refactor init selected option menu
    appTittleText.setText(TITTLE_TRANSFERS);
    homeControllerNodes
        .addNode(filesAppContent)
        .addNode(connectionAppContent)
        .addNode(filesOptionContent)
        .addNode(stackAppContainer)
        .addNode(connectionOptionContent)
        .addNode(mainMenu)
        .addNode(searchOptionContent)
        .addNode(searchAppContent)
        .addNode(transfersOptionContent)
        .addNode(transfersAppContent)
        .addNode(appTittleText)
        .addNode(settingsAppContent)
        .addNode(settingsOptionContent)
    ;
  }

}
