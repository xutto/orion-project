package com.mac.orion.domain.model;

import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public class SearchResource {

  private final UUID id;
  private final String snippet;
  private final Peer peerFinder;
  private final int depth;
  private final int limit;
}
