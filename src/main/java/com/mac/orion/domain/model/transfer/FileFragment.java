package com.mac.orion.domain.model.transfer;

import com.mac.orion.domain.model.Hash;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public class FileFragment {

  private final Hash hash;
  private final Integer index;
  private Integer size;

  /* only used for host node to extract the chunk data*/
  private final byte[] fragmentData;

}
