package com.mac.orion.infrastructure.configuration;

import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.infrastructure.ui.command.BootstrapCopyCommand;
import com.mac.orion.infrastructure.ui.command.BootstrapDeleteCommand;
import com.mac.orion.infrastructure.ui.creation.BootstrapListCreator;
import com.mac.orion.infrastructure.ui.creation.SettingsAlertsCreator;
import com.mac.orion.infrastructure.ui.creation.TooltipsCreator;
import com.mac.orion.infrastructure.ui.events.EventsAssembler;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
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

  @Bean
  public BootstrapListCreator bootstrapListCreator(
      ControllerNodes settingsControllerNodes,
      BootstrapUseCase bootstrapUseCase,
      BootstrapCopyCommand bootstrapCopyCommand,
      BootstrapDeleteCommand bootstrapDeleteCommand,
      EventsAssembler eventsAssembler) {
    return new BootstrapListCreator(settingsControllerNodes, bootstrapUseCase, bootstrapCopyCommand,
        bootstrapDeleteCommand, eventsAssembler);
  }
}
