package com.mac.orion.domain.model.transfer;

import java.util.BitSet;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class FileAvailability {

  private final BitSet availability;

}
