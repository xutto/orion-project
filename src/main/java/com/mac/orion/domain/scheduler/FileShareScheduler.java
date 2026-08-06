package com.mac.orion.domain.scheduler;

import com.mac.orion.application.in.SchedulerProcessHandler;
import com.mac.orion.application.out.FileSharerDialerUseCase;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Peer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileShareScheduler implements SchedulerProcessHandler {

  public static final int DEFAULT_INTERVAL = 15_000; // todo config (File Share update interval )
  private final AtomicBoolean running = new AtomicBoolean(false);
  private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  private final RoutingTable routingTable;
  private final FileSharerDialerUseCase fileSharerDialerUseCase;

  @Override
  public void startProcess() {

    if (running.compareAndSet(false, true)) {
      executor.submit(this::runScheduler);
    }

  }

  @Override
  public void stopProcess() {
    running.set(false);
  }


  private void runScheduler() {

    while (running.get()) {
      routingTable.getAllPeers().forEach(peer -> {

        if (!running.get()) {
          return;
        }

        executor.submit(() -> {
          sharingFiles(peer);
        });


      });


      try {
        Thread.sleep(DEFAULT_INTERVAL); // todo config
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }


  }

  private void sharingFiles(Peer peer) {
    log.info("sharing files to: {}", peer.getId());
    peer.getAddress()
        .forEach(address -> log.info("with address: {}", address.buildAddressChain()));
    fileSharerDialerUseCase.sendSharedFiles(peer);
  }


}
