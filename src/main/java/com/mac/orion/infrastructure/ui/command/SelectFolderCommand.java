package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.application.in.SettingsManagementUseCase;
import com.mac.orion.domain.model.settings.Directories;
import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.domain.share.NodesIdentifierConstants;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class SelectFolderCommand implements Command<Event> {

  public static final String SELECT_DIRECTORY_TITTLE = "Seleccionar directorio";
  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();
  private final ControllerNodes settingsControllerNodes;
  private final Settings settings;
  private final SettingsManagementUseCase configurationManagementService;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED, List.of(
        settingsControllerNodes.getNode(NodesIdentifierConstants.EXPLORE_DOWNLOADS_FOLDER_BUTTON),
        settingsControllerNodes.getNode(NodesIdentifierConstants.EXPLORE_TEMP_FOLDER_BUTTON)));
    compatibilities.put(EventType.ON_BOOT, List.of());
  }

  @Override
  public void execute(Event event) {

    final Node sourceButton = (Node) event.getSource();
    final File directory = determinePathByButtonSelected(sourceButton);

    if (directory != null){
      final Directories directoriesNew = determineWriteLabel(sourceButton, directory);
      settings.setDirectories(directoriesNew);
      configurationManagementService.saveSettings(settings);
    }

  }

  @Override
  public void execute() {
    final Label downloadsLabel = settingsControllerNodes.getNode(
        NodesIdentifierConstants.LABEL_DOWNLOADS_FOLDER, Label.class);
    final Label tempLabel = settingsControllerNodes.getNode(
        NodesIdentifierConstants.LABEL_TEMP_FOLDER, Label.class);
    Platform.runLater(() -> {
      downloadsLabel.setText(settings.getDirectories().download());
      tempLabel.setText(settings.getDirectories().temp());
    });
  }

  private File determinePathByButtonSelected(Node sourceButton) {
    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle(SELECT_DIRECTORY_TITTLE);
    return directoryChooser.showDialog(sourceButton.getScene().getWindow());
  }

  private Directories determineWriteLabel(Node sourceButton, File directory) {
    final Directories directoriesLoad = settings.getDirectories();
    return switch (sourceButton.getId()) {
      case NodesIdentifierConstants.EXPLORE_DOWNLOADS_FOLDER_BUTTON -> {
        settingsControllerNodes.getNode(
                NodesIdentifierConstants.LABEL_DOWNLOADS_FOLDER, Label.class)
            .setText(directory.getAbsolutePath());
        yield new Directories(directoriesLoad.scan(),
            directory.getAbsolutePath(), directoriesLoad.temp());
      }
      case NodesIdentifierConstants.EXPLORE_TEMP_FOLDER_BUTTON -> {
        settingsControllerNodes.getNode(
                NodesIdentifierConstants.LABEL_TEMP_FOLDER, Label.class)
            .setText(directory.getAbsolutePath());
        yield new Directories(directoriesLoad.scan(),
            directoriesLoad.download(), directory.getAbsolutePath());
      }
      default -> directoriesLoad;
    };

  }

}


