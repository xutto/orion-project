package com.mac.orion.application.in;

import com.mac.orion.domain.model.settings.Settings;

public interface SettingsManagementUseCase {

  void reloadSettings();

  void saveSettings(Settings settings);

}
