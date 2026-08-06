package com.mac.orion.application.service;


import com.mac.orion.application.in.UpdateFileRoutingTableUseCase;
import com.mac.orion.domain.dht.FileRoutingTable;
import com.mac.orion.domain.model.File;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UpdateFileRoutingTableService implements UpdateFileRoutingTableUseCase {

  private final FileRoutingTable fileRoutingTable;

  @Override
  public void updateFileRoutingTable(Set<File> files) {
    log.info("Updating file routing table with files: [{}]", files.size());
    files.forEach(fileRoutingTable::addFile);
  }

}
