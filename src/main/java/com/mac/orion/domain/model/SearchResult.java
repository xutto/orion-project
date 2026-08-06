package com.mac.orion.domain.model;

import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class SearchResult {

  private final UUID id;
  private final Set<File> files;

}
