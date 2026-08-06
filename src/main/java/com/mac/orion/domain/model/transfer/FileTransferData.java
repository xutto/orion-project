package com.mac.orion.domain.model.transfer;

import com.mac.orion.domain.model.Hash;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public class FileTransferData {

  @NonNull
  private final MessageType messageType;
  private final Hash hash;
  private final Long size;
//  private final FileGet fileGet;
  private final FileAvailability fileAvailability;
  private final NextWindow nextWindow;
  private final FileFragment fileFragment;
  private final FileChunk fileChunk;
  private final TransferEnd transferEnd;
  private final FileError fileError;


}
