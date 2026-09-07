package com.mac.orion.infrastructure.configuration;

import com.mac.orion.infrastructure.ui.creation.SettingsAlertsCreator;
import com.mac.orion.infrastructure.ui.creation.TooltipsCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreatorConfiguration {

  @Bean
  public TooltipsCreator tooltipsCreator() {
    final TooltipsCreator tooltipsCreator = new TooltipsCreator();
//    tooltipsCreator.createHelpersTooltipSettings();
    return tooltipsCreator;
  }

  @Bean
  public SettingsAlertsCreator settingsAlertsCreator() {
    return new SettingsAlertsCreator();
  }
}
