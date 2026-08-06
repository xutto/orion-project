package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.infrastructure.ui.events.EventType;
import java.util.List;
import java.util.Map;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public interface Command<T extends Event> {

    /**
     * get compatibilities from map include eventType(jfx) and nodes
     * @return Map of compatibilities
     */
    Map<EventType, List<Node>> getCompatibilities();

    /**
     * Configure compatibility with com.mac.orion.events.Events
     */
    void configureCompatibility();

    /**
     * Execute command WHEN HAVE JAVAFX EVENT, for example on mouse click, hove or another uses
     * If the command require multiples process can use a Service layer
     */
    default void execute(T event){
        throw new  UnsupportedOperationException("Command must implement execute(T event)");
    }

    /**
     * Execute command when HAVE NOT JAVAFX event, for example on BOOT application or
     * execute TIMED, or another uses
     */
    default void execute(){
        throw new  UnsupportedOperationException("Command must implement execute()");
    }

    /**
     * Checks if the given {@code Parent} node or any of its descendants currently has focus.
     *
     * @param parent the parent node to check for focus within its hierarchy
     * @return {@code true} if the parent node or any of its descendants has focus,
     *         {@code false} otherwise
     */
    default boolean hasFocusInside(Parent parent) {
        Scene scene = parent.getScene();

        if (scene == null) {
            return false;
        }

        Node focused = scene.getFocusOwner();

        while (focused != null) {
            if (focused == parent) {
                return true;
            }

            focused = focused.getParent();
        }

        return false;
    }
}
