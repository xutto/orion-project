package com.mac.orion;

import com.mac.orion.infrastructure.ui.loader.FontBootstrap;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import java.io.InputStream;
import java.util.Optional;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;


@Slf4j
public class DemoApplicationLoader extends Application {

  public static final String UI_DEMO_DEFAULT_FXML = "ui/demo/default.fxml";
  private Parent root;

  public static void boot(String[] args) {

    launch(DemoApplicationLoader.class, args);
  }

  @Override
  public void init() throws Exception {

    final Parameters parameters = getParameters();
    parameters.getNamed().forEach((key, value) -> log.info("Parameter: {} = {}", key, value));
    final String fxmlToLoad = Optional.ofNullable(parameters.getNamed().get("fxml"))
        .orElse(UI_DEMO_DEFAULT_FXML);

    FXMLLoader fxmlLoader = new FXMLLoader();
    final Resource resource = new ClassPathResource(fxmlToLoad);
    try (InputStream inputStream = resource.getInputStream()) {
      root = fxmlLoader.load(inputStream);
    }
  }

  @Override
  public void start(Stage stage) throws Exception {

    // fonts loading, to add new fonts, must be defined in FontBootstrap loader
    FontBootstrap.loadAll();

    // user agent material fx settings
    UserAgentBuilder.builder()
        .themes(
            JavaFXThemes.MODENA) // Optional if you don't need JavaFX's default theme, still recommended though
        .themes(MaterialFXStylesheets.forAssemble(
            true)) // Adds the MaterialFX's default theme. The boolean argument is to include legacy controls
        .setDeploy(true) // Whether to deploy each theme's assets on a temporary dir on the disk
        .setResolveAssets(true) // Whether to try resolving @import statements and resources urls
        .build() // Assembles all the added themes into a single CSSFragment (very powerful class check its documentation)
        .setGlobal();

    stage.setTitle("Orion Application");
    stage.setScene(new Scene(root, 1280, 800));
    stage.show();

  }


}
