package com.mac.orion.domain.model.transfer;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class NextWindow {

  private final Integer index;
  private final Boolean last;

}
