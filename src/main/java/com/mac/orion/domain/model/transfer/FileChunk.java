package com.mac.orion.domain.model.transfer;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class FileChunk {

  private final Integer index;
  private final byte[] data;
  private Boolean last;

}
