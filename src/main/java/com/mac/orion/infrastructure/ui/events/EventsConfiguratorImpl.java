package com.mac.orion.infrastructure.ui.events;

import com.mac.orion.infrastructure.ui.command.Command;
import javafx.event.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventsConfiguratorImpl implements Events {

  private final List<Command<Event>> commands;
  private final EventsAssembler eventsAssembler;

  @Override
  public void configure() {
    eventsAssembler.configureCommandEvents(commands);
  }
}
