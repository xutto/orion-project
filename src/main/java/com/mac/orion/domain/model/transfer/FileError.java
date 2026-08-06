package com.mac.orion.domain.model.transfer;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class FileError {

  private final String code;
  private final String message;
  private final Long fragment;

}
