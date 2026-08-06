package com.mac.orion.domain.model;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public final class File { //todo falta poner la propiedad "status" para saber si esta en la BD por que es propietario (store) , se está descargando (pending)

  private final Long id;
  private final Hash hash;
  @EqualsAndHashCode.Exclude
  private final Set<String> names;
  private final String path;
  private final Long size;
  private final int score;
  @EqualsAndHashCode.Exclude
  private final Map<String, Peer> peers;
  private final Status status;

}
