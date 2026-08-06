package com.mac.orion.infrastructure.ui.events;

import lombok.Getter;

@Getter
public enum EventType {

  MOUSE_CLICKED("MOUSE_CLICKED"),
  ON_BOOT("ON_BOOT"),
  KEY_RELEASED("KEY_RELEASED"),
  ON_HOVER("on_mouse_hover"), //todo poner el nombre exacto de javafx del evento
  ON_ACTION("on_action")
//    ON_DOUBLE_CLICK,
//    ON_HOVER;
  ;
  private final String name;

  EventType(String name) {
    this.name = name;
  }
}
