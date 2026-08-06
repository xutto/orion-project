package com.mac.orion.application.service;

import com.mac.orion.application.in.KadConnectorUseCase;
import com.mac.orion.application.in.SchedulerProcessHandler;
import com.mac.orion.domain.Bootable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KadConnectorService implements KadConnectorUseCase, Bootable {

  private final SchedulerProcessHandler peerDiscoveryScheduler;

  @Override
  public void connectAnnouncePeer() {

    // start a discovery process
    peerDiscoveryScheduler.startProcess();

  }

  @Override
  public void boot() {
    this.connectAnnouncePeer();
  }
}
