package com.mac.orion.application.service;

import com.mac.orion.application.in.FileSharingConectorUseCase;
import com.mac.orion.application.in.SchedulerProcessHandler;
import com.mac.orion.domain.Bootable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileSharingConnectorService implements FileSharingConectorUseCase, Bootable {

  private final SchedulerProcessHandler fileShareScheduler;

  @Override
  public void connectFileSharing() {
    // start the file sharing process
    fileShareScheduler.startProcess();
  }

  @Override
  public void boot() {
    this.connectFileSharing();
  }
}
