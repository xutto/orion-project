package com.mac.orion.domain.model;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Builder( toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public class Transfer {

  private final Set<String> names;
  private final Hash hash;
  private final int totalFragments;
  private final long size;
  private final long completed; // todo must calculate
  private final long speed; // todo must calculate
  private final int peers; // todo unknown info now
  private final List<Integer> transfersCompleted;
  private final Set<Integer> transfersOnFly;

}
