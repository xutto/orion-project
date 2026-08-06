package com.mac.orion.domain.model.transfer;

import lombok.Getter;

@Getter
public enum MessageType {

  FILE_GET(0),
  FILE_AVAILABILITY(1),
  NEXT_WINDOW(2),
  FILE_CHUNK(3),
  //  WINDOW_END = 4;
  TRANSFER_END(4),
  FILE_ERROR(5),
  UNRECOGNIZED(6);

  private final int value;

  MessageType(int value) {
    this.value = value;
  }

}
