package com.mac.orion.domain.model.transfer;

import java.util.BitSet;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder( toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public class Sidecar {


  private final Integer version;
  private final Integer totalFragments;
  private final BitSet data;


}
