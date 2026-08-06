package com.mac.orion.infrastructure.ui.demo;

import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@EqualsAndHashCode
@ToString
public final class Item {

  private String displayName;
  private Path path;
  private Boolean listening;

  public Item(String displayName, Path path, Boolean listening) {
    this.displayName = displayName;
    this.path = path;
    this.listening = listening;
  }


}
