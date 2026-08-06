package com.mac.orion.infrastructure.ui.events;

import com.mac.orion.infrastructure.ui.command.Command;
import java.util.List;
import javafx.event.Event;

public interface EventsAssembler {


  /**
   * Configures command events by associating specified commands with their compatible nodes and event types.
   * This method processes the provided list of commands and dynamically sets event handlers based on the
   * defined compatibilities in each command.
   *
   * @param eventCommands a list of {@code Command<Event>} objects representing commands to be configured.
   *                      Each command contains a map of {@code EventType} to {@code List<Node>} that defines
   *                      its compatibilities. For example, a command may define that it should be executed
   *                      when a specific node triggers an event of a certain type.
   */
  void configureCommandEvents(List<Command<Event>> eventCommands);
}
