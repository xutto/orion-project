package com.mac.orion.domain.scheduler;

import com.mac.orion.application.in.SchedulerProcessHandler;
import com.mac.orion.application.in.TransferControlUseCase;
import com.mac.orion.application.service.publisher.DownloadingPublisherService;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Transfer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadingCheckDataScheduler implements SchedulerProcessHandler {

  private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private final Map<Hash, AtomicBoolean> activeTasks = new ConcurrentHashMap<>();
  private final DownloadingPublisherService downloadingPublisherService;
  private final TransferControlUseCase transferControlService;


  @Override
  public void startProcess(Object arg) {

    final Hash hash = Optional.ofNullable(arg).map(Hash.class::cast)
        .orElseThrow(() -> new IllegalArgumentException("Hash cannot be null"));

    AtomicBoolean isRunning = activeTasks.computeIfAbsent(hash, k -> new AtomicBoolean(true));
    executor.submit(() -> runScheduler(hash, isRunning));

  }

  @Override
  public void stopProcess(Object arg) {
    final Hash hash = Optional.ofNullable(arg).map(Hash.class::cast)
        .orElseThrow(() -> new IllegalArgumentException("Hash cannot be null"));
    AtomicBoolean isRunning = activeTasks.get(hash);
    if (isRunning != null) {
      activeTasks.remove(hash);
      isRunning.set(false);
//      transferPublisherService.publish(OperationsType.REMOVE, dataTransfer);
    }
  }

  private void runScheduler(Hash hash, AtomicBoolean isRunning) {


    try {
      while (isRunning.get()) {


        // todo get transfer by hash
        // todo get Bitset and update Trransfer
        // todo publish transfer
        final Transfer dataTransfer = transferControlService.getDataTransfer(hash);

        Thread.sleep(1000); //todo a config (intervalo de actualizacion de transferencias)
        if (dataTransfer == null) {
          log.warn("Transfer not found for hash: {}", hash);
          log.warn("scan transfer agent stoping");
          isRunning.set(false);
        } else {
          downloadingPublisherService.publish(OperationsType.DOWNLOADING, dataTransfer);
        }


      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      isRunning.set(false);
      activeTasks.remove(hash);
//      transferPublisherService.publish(OperationsType.REMOVE, );
      log.info("Thread by hash:{} finished.", hash);
    }

  }
}
