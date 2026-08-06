package com.mac.orion.infrastructure.ui.events;

import com.mac.orion.infrastructure.ui.command.Command;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventsAssemblerImpl implements EventsAssembler {

  private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private final Set<Node> onMouseClickedNodesConfigured = new HashSet<>();
  private final Set<Node> onMouseEnteredNodesConfigured = new HashSet<>();

  @Override
  public void configureCommandEvents(List<Command<Event>> eventCommands) {

    eventCommands.forEach(command -> {
      final Map<EventType, List<Node>> compatibilities = command.getCompatibilities();
      compatibilities.forEach((e, nodes) -> executor.submit(() -> {
        switch (e) {
          case EventType.ON_BOOT -> command.execute();
          case EventType.MOUSE_CLICKED ->
              nodes.stream().filter(n -> !onMouseClickedNodesConfigured.contains(n)).forEach(n -> {
                n.setOnMouseClicked(mouseEvent -> determineCommandsByNode(eventCommands, n, e)
                    .forEach(c -> c.execute(mouseEvent)));
                onMouseClickedNodesConfigured.add(n);
              });
          case EventType.KEY_RELEASED ->
              nodes.forEach(n -> n.getScene().addEventFilter(KeyEvent.KEY_RELEASED,
                  keyEvent -> determineCommandsByKeys(eventCommands).forEach(
                      c -> c.execute(keyEvent))));
//   case EventType.ON_HOVER -> nodes1.forEach(n -> n.setOnMouseEntered(eventCommand::execute));
        }
      }));

    });

  }

  private List<Command<Event>> determineCommandsByNode(List<Command<Event>> eventCommands,
      Node node, EventType eventType) {
    return eventCommands.stream().filter(c -> c.getCompatibilities().containsKey(eventType))
        .filter(c -> c.getCompatibilities().get(eventType).stream().map(Node::getId).toList()
            .contains(node.getId()))
        .toList();
  }

  private List<Command<Event>> determineCommandsByKeys(List<Command<Event>> eventCommands) {
    return eventCommands.stream()
        .filter(c -> c.getCompatibilities().containsKey(EventType.KEY_RELEASED))
        .toList();

  }

}

//    eventCommands.forEach(command -> {
//
//      final Map<EventType, List<Node>> compatibilities = command.getCompatibilities();
//      compatibilities.forEach((e, nodes) -> executor.submit(() -> {
//        switch (e) {
//          case EventType.ON_BOOT -> command.execute();
//          case EventType.MOUSE_CLICKED -> nodes.forEach(n -> n.setOnMouseClicked(command::execute));
/// /                    case EventType.ON_HOVER -> nodes1.forEach(n ->
/// n.setOnMouseEntered(eventCommand::execute));
//        }
//      }));
//
//    });
