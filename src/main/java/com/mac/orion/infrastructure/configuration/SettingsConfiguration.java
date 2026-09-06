package com.mac.orion.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.domain.model.settings.Client;
import com.mac.orion.domain.model.settings.Directories;
import com.mac.orion.domain.model.settings.P2P;
import com.mac.orion.domain.model.settings.Settings;
import com.mac.orion.domain.share.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Configuration
public class SettingsConfiguration {

  public static final String ORION_FOLDER_ROOT = ".orion";

  //  private static final Path base = Paths.get(System.getenv("USERPROFILE"), ".Orion");

  @Value("${orion.p2p.bootstrap-ip}")
  private String bootstrapAddress;
  @Value("${orion.p2p.bootstrap-port}")
  private String bootstrapPort;
  @Value("${orion.p2p.bootstrap-id}")
  private String bootstrapId;
  @Value("${orion.p2p.limitK}")
  private Integer limitK;
  @Value("${orion.user-profile:${USERPROFILE}}")
  private String userProfile;

  @Bean
  public Settings settings() {

    Path base = Paths.get(userProfile, ORION_FOLDER_ROOT);
    final Settings settings = new Settings();

    try {
      if (!Files.exists(base)) {
        Files.createDirectories(base);
      }else {
        // fix case-sensitive folder if exists with capitalizing.
        base.toFile().renameTo(base.toFile());
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize configuration directory", e);
    }

    Resource resource = new ClassPathResource("default-settings.json");
    try (InputStream in = resource.getInputStream()) {
      Path configPath = Paths.get(base.toString(), "settings.json");

      if (Files.exists(configPath)) {
        ObjectMapper mapper = new ObjectMapper();
        final Settings settingsLoaded = mapper.readValue(configPath.toFile(), Settings.class);
        settings.setDirectories(settingsLoaded.getDirectories());
        settings.setP2p(settingsLoaded.getP2p());
      } else {
        ObjectMapper mapper = new ObjectMapper();
        final Settings settingsLoadedFromDefault = mapper.readValue(in, Settings.class);
        final List<String> scanPaths = settingsLoadedFromDefault.getDirectories().scan().stream()
            .peek(this::createDirectoryWithBase)
            .map(scan -> Paths.get(base.toString(), scan).toAbsolutePath().toString())
            .toList();

        createDirectoryWithBase(settingsLoadedFromDefault.getDirectories().download());
        createDirectoryWithBase(settingsLoadedFromDefault.getDirectories().temp());
        final Directories defaultDirectories = new Directories(scanPaths,
            Paths.get(base.toString(), settingsLoadedFromDefault.getDirectories().download())
                .toAbsolutePath().toString(),
            Paths.get(base.toString(), settingsLoadedFromDefault.getDirectories().temp())
                .toAbsolutePath().toString());

        settings.setDirectories(defaultDirectories);
        settings.setP2p(getDefaultP2P());

        mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), settings);
      }

      return settings;
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize configuration file", e);
    }


  }

  private void createDirectoryWithBase(String directory) {
    Path base = Paths.get(userProfile, ORION_FOLDER_ROOT);
    final Path path = Paths.get(base.toString(), directory);
    final File fileDirectory = path.toFile();
    boolean created = false;
    if (!fileDirectory.exists()) {
      created = fileDirectory.mkdirs();
    }
    if (created) {
      log.info("Created directory: {}", directory);
    } else {
      log.debug("Directory already exists: {}", directory);
    }

  }

  private P2P getDefaultP2P() {

    final Client client = new Client(String.valueOf(Constants.P2P_PORT_DEFAULT), limitK);
    final Bootstrap bootstrap = new Bootstrap(bootstrapPort, bootstrapId, bootstrapAddress);
    return new P2P(client, bootstrap);
  }


}
