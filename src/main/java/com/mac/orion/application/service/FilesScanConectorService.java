package com.mac.orion.application.service;

import com.mac.orion.application.in.FilesScanConectorUseCase;
import com.mac.orion.application.in.SchedulerProcessHandler;
import com.mac.orion.domain.Bootable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilesScanConectorService implements FilesScanConectorUseCase, Bootable {

  private final SchedulerProcessHandler filesScanScheduler;

  @Override
  public void boot() {
    this.connectFilesScan();
  }

  @Override
  public void connectFilesScan() {

    // start process files scan
    filesScanScheduler.startProcess();
  }
}
