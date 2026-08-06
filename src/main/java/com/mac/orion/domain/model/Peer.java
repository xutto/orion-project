package com.mac.orion.domain.model;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@ToString
@EqualsAndHashCode
public final class Peer { // todo mejorar el modelo de Peer

  private final String id;

  @Deprecated() // todo se usa el ID
  private final String hash;
  private final Set<Address> address;
  private final Instant lastSeen;
  private final BigInteger distance;

}
