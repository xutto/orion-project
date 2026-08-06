package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class PrintMessageCommand<T extends Event> implements Command<T> {

    private final ControllerNodes homeControllerNodes;
    private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

    @Override
    public Map<EventType, List<Node>> getCompatibilities() {
        return this.compatibilities;
    }

    @Override
    public void configureCompatibility() {
        compatibilities.put(EventType.ON_BOOT, List.of());
    }

    @Override
    public void execute() {
        log.info("create command when boot application, get inject home controller NODES: {}", homeControllerNodes.getNodes().size());
    }
}
