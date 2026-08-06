package com.mac.orion;

import com.mac.orion.infrastructure.ui.command.Command;
import com.mac.orion.infrastructure.ui.creation.Creator;
import com.mac.orion.infrastructure.ui.events.Events;
import com.mac.orion.infrastructure.ui.loader.FontBootstrap;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.mac.orion.domain.share.NodesIdentifierConstants.UI_RESOURCE_HOME_FXML;

@Slf4j
public class DesktopApplicationLoader extends Application {

  public static final String ENABLED_UI_OPTION = "--enabled-ui=";
//  public static final String BEAN_FXML_LOADER = "fxmlLoader";
  private static String[] argsBuild = new String[0];
  private ConfigurableApplicationContext context;
  private Parent root;
  private static Boolean enabledUI = true;


  public static void boot(String[] args) {
    System.out.println("Orion Application is starting with args: " + String.join(" ", args));
    log.info("Orion Application is starting with args: {}", String.join(" ", args));
    if (args.length > 0) {
      argsBuild = args;

      new ArrayList<>(List.of(args)).
          stream().filter(arg -> arg.startsWith(ENABLED_UI_OPTION))
          .findFirst()
          .ifPresent(arg -> {
            final String[] split = arg.split("=");
            enabledUI = Boolean.parseBoolean(split[1]);
          });
    }

    if (enabledUI) {
      launch(DesktopApplicationLoader.class, argsBuild);
    } else {
      SpringApplication.run(OrionApplication.class, argsBuild);

      log.info("Orion Application is starting with args: {}, IN [NO-UI] MODE",
          String.join(" ", argsBuild));
      log.info("The enabled UI is: {} \n", enabledUI);
      try {
        Thread.sleep(Integer.MAX_VALUE);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }


  }

  @Override
  public void init() throws Exception {

    // springboot run
    this.context = SpringApplication.run(OrionApplication.class, argsBuild);
    // java fx load root UI

    FXMLLoader fxmlLoader = new FXMLLoader();
    fxmlLoader.setControllerFactory(context::getBean);
    final Resource resource = new ClassPathResource(UI_RESOURCE_HOME_FXML);
    try (InputStream inputStream = resource.getInputStream()) {
      root = fxmlLoader.load(inputStream);
    }
  }

  @Override
  public void start(Stage stage) {

    // load controller nodes as beans, configures, initializations, etc..., must be correct order to load because the context is dependent of that
    stage.setOnShown(windowEvent -> {

      // configure compatibility commands
      context.getBeansOfType(Command.class).forEach((name, bean) -> bean.configureCompatibility());

      // configure executable events
      context.getBeansOfType(Events.class).forEach((e, t) -> t.configure());

      // configure node creators
      context.getBeansOfType(Creator.class).forEach((name, bean) -> bean.init());

    });

    // fonts loading, to add new fonts, must be defined in FontBootstrap loader
    FontBootstrap.loadAll();

    // user agent material fx settings
    UserAgentBuilder.builder()
        .themes(JavaFXThemes.MODENA) // Optional if you don't need JavaFX's default theme, still recommended though
        .themes(MaterialFXStylesheets.forAssemble(true)) // Adds the MaterialFX's default theme. The boolean argument is to include legacy controls
        .setDeploy(true) // Whether to deploy each theme's assets on a temporary dir on the disk
        .setResolveAssets(true) // Whether to try resolving @import statements and resources urls
        .build() // Assembles all the added themes into a single CSSFragment (very powerful class check its documentation)
        .setGlobal();

    stage.setTitle("Orion Application");
    stage.setScene(new Scene(root, 1280, 800));
    stage.show();
  }

  @Override
  public void stop() {

    Platform.runLater(() -> {
      context.stop();
      context.close();
      Platform.exit();
      System.exit(0);
    });

  }
}
