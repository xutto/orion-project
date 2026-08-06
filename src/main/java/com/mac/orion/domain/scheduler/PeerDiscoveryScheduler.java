package com.mac.orion.domain.scheduler;

import com.mac.orion.application.in.SchedulerProcessHandler;
import com.mac.orion.application.out.KadDialerUseCase;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Peer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PeerDiscoveryScheduler implements SchedulerProcessHandler {

  public static final int DEFAULT_DISCOVERY_INTERVAL = 15_000; // todo config
  private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private static final int PARALLEL_THREADS_PERMIT = 5; // todo config
  private static final Semaphore semaphore = new Semaphore(PARALLEL_THREADS_PERMIT);
  private final AtomicBoolean running = new AtomicBoolean(false);


  private final RoutingTable routingTable;
  private final KadDialerUseCase kadDialerUseCase;

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
        if (semaphore.tryAcquire()) {
          executor.submit(() -> {
            try {
              attemptConnection(peer);
            } finally {
              semaphore.release();
            }
          });
        }

      });

      try {
        Thread.sleep(DEFAULT_DISCOVERY_INTERVAL); // todo config
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

    }
  }

  private void attemptConnection(Peer targetPeer) {
    log.info("\uD83D\uDD0D Conectando a: {}", targetPeer.getId());
    targetPeer.getAddress()
        .forEach(address -> log.info("with address: {}", address.buildAddressChain()));
    log.info("peers before: [{}] \n", routingTable.getAllPeers().size());
    kadDialerUseCase.sendAnnounceAndDiscovery(targetPeer);
  }


}
