package com.mac.orion.infrastructure.ui.controller;

import com.mac.orion.application.in.FileSharingConectorUseCase;
import com.mac.orion.infrastructure.ui.demo.Item;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.net.URL;
import java.util.ResourceBundle;

import static com.mac.orion.domain.share.NodesIdentifierConstants.STYLE_CLASS_SHARED_TREEVIEW_CELL;

@Configuration
@RequiredArgsConstructor
public class SettingsController implements UIController {

  private final ControllerNodes settingsControllerNodes;
  private final FileSharingConectorUseCase fileSharingConectorUseCase;

  @FXML
  private Node settingsCloseIcon;
  @FXML
  private Node mainSettings;
  @FXML
  private Node exploreDownloadsFolderButton;
  @FXML
  private Node labelDownloadsFolder;
  @FXML
  private Node exploreTempFolderButton;
  @FXML
  private Node labelTempFolder;
  @FXML
  private TreeView<Item> sharedTreeview;
  @FXML
  private Node settingsDirectoriesContainerPane;
  @FXML
  private Node settingsConnectionContainerPane;
  @FXML
  private Node settingsContentDirectories;
  @FXML
  private Node settingsContentConnection;
  @FXML
  private Node settingsViewerPane;

  // Bootstrap fields
  @FXML
  private TextField bootstrapIpField;
  @FXML
  private TextField bootstrapPortField;
  @FXML
  private TextField bootstrapIdField;
  @FXML
  private Button addBootstrapButton;
  @FXML
  private Node bootstrapListContainer;
  @FXML
  private Label settingsLabelIp;

  // Connection info
  @FXML
  private Label settingsLabelPort;
  @FXML
  private Label settingsLabelNodeId;
  @FXML
  private Node settingsCopyNodeId;

  // LimitK spinner
  @FXML
  private Spinner<Integer> limitKSpinner;

  // Help icons
  @FXML
  private Node connectionDataHelpIcon;
  @FXML
  private Node bootstrapNodesHelpIcon;
  @FXML
  private Node limitKHelpIcon;

  @Override
  public void initialize(URL location, ResourceBundle resources) {


    sharedTreeview.setCellFactory(tv -> {
      CheckBoxTreeCell<Item> cell = new CheckBoxTreeCell<>(
          item -> ((CheckBoxTreeItem<Item>) item).selectedProperty(),
          new StringConverter<>() {
            @Override
            public String toString(TreeItem<Item> item) {
              if (item == null || item.getValue() == null) {
                return "";
              }
              return item.getValue().getDisplayName();
            }

            @Override
            public TreeItem<Item> fromString(String text) {
              return null;
            }
          }
      );

      cell.getStyleClass().add(STYLE_CLASS_SHARED_TREEVIEW_CELL);

      return cell;
    });

    // Configure the limitK spinner
    limitKSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 20));

    settingsControllerNodes
        .addNode(settingsCloseIcon)
        .addNode(mainSettings)
        .addNode(exploreDownloadsFolderButton)
        .addNode(labelDownloadsFolder)
        .addNode(exploreTempFolderButton)
        .addNode(labelTempFolder)
        .addNode(sharedTreeview)
        .addNode(settingsDirectoriesContainerPane)
        .addNode(settingsConnectionContainerPane)
        .addNode(settingsContentDirectories)
        .addNode(settingsContentConnection)
        .addNode(settingsViewerPane)
        .addNode(settingsLabelIp)
        // Connection info
        .addNode(settingsLabelPort)
        .addNode(settingsLabelNodeId)
        .addNode(settingsCopyNodeId)
        // Bootstrap fields
        .addNode(bootstrapIpField)
        .addNode(bootstrapPortField)
        .addNode(bootstrapIdField)
        .addNode(addBootstrapButton)
        .addNode(bootstrapListContainer)
        // LimitK spinner
        .addNode(limitKSpinner)
        // Help icons
        .addNode(connectionDataHelpIcon)
        .addNode(bootstrapNodesHelpIcon)
        .addNode(limitKHelpIcon)
    ;
  }
}
