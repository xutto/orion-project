package com.mac.orion.application.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mac.orion.application.in.SettingsManagementUseCase;
import com.mac.orion.domain.model.settings.Settings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.mac.orion.infrastructure.configuration.SettingsConfiguration.ORION_FOLDER_ROOT;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettingsManagementService implements SettingsManagementUseCase {

  public static final String SETTINGS_JSON = "settings.json";

  private final Settings settings;

  @Value("${orion.user-profile:${USERPROFILE}}")
  private String userProfile;

  @Override
  public void reloadSettings() {
    Path base = Paths.get(userProfile, ORION_FOLDER_ROOT);

    try {
      Path configPath = Paths.get(base.toString(), SETTINGS_JSON);

      ObjectMapper mapper = new ObjectMapper();
      final Settings settingsLoaded = mapper.readValue(configPath.toFile(), Settings.class);
      settings.setDirectories(settingsLoaded.getDirectories());
      settings.setP2p(settingsLoaded.getP2p());
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize configuration file", e);
    }

  }

  @Override
  public void saveSettings(Settings settingsNew) {
    Path base = Paths.get(userProfile, ORION_FOLDER_ROOT);
    Path configPath = Paths.get(base.toString(), SETTINGS_JSON);
    ObjectMapper mapper = new ObjectMapper();

    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), settingsNew);
      settings.setDirectories(settingsNew.getDirectories());
      settings.setP2p(settingsNew.getP2p());
    } catch (IOException e) {
      log.error("Cannot save settings file: {}", SETTINGS_JSON, e);
      throw new RuntimeException(e);
    }

  }
}
