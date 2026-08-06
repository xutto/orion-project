package com.mac.orion.domain.model.settings;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Settings {

  private Directories directories;
  private P2P p2p;

}
