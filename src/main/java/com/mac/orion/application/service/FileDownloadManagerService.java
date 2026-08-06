package com.mac.orion.application.service;

import static com.mac.orion.domain.dht.OperationsType.RECLAIM;
import static com.mac.orion.domain.dht.OperationsType.RETRIEVE;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.Bootable;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.infrastructure.p2p.dial.FileTransferDialer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadManagerService implements Bootable {

  private final FileTransferDialer fileTransferDialer;
  private final Publisher<File> fileDownloadPublisherService;

  // Almacén de colas: Hash -> Cola de peticiones pendientes
  private final Map<Hash, BlockingQueue<Runnable>> pendingQueues = new ConcurrentHashMap<>();

  // Semáforos para controlar los "slots" de fragmentos en vuelo (sustituye al AtomicInteger)
  private final Map<Hash, Semaphore> fileSemaphores = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<Hash, File> downloadingFiles = new ConcurrentHashMap<>();

  private final Map<Hash, Thread> workers = new ConcurrentHashMap<>();
  private static final int MAX_CONCURRENT_FRAGMENTS = 10;

  @Override
  public void boot() {

    fileDownloadPublisherService.subscribe(OperationsType.COMPLETE, f -> stopAndCleanup(f.getHash()));

    // Suscripción al RECLAIM (Inicio de descarga)
    fileDownloadPublisherService.subscribe(RECLAIM, f -> {
      downloadingFiles.put(f.getHash(), f);
      initDownloadWorker(f.getHash()); // Iniciamos el consumidor de la cola

      f.getPeers().forEach((id, peer) ->
          enqueueRequest(f.getHash(), () -> fileTransferDialer.sendInitialDownloadFileProcess(f.getHash(), peer)));
    });

    // Suscripción al RETRIEVE (Nuevos intentos de pesca)
    fileDownloadPublisherService.subscribe(RETRIEVE, f -> {
      File fileData = downloadingFiles.get(f.getHash());
      if (fileData != null) {
        fileData.getPeers().forEach((id, peer) ->
            enqueueRequest(f.getHash(), () -> fileTransferDialer.sendInitialDownloadFileProcess(f.getHash(), peer)));
      }
    });

    // Suscripción al SAVE (Slot liberado)
    fileDownloadPublisherService.subscribe(OperationsType.SAVE, f -> {
      // Al hacer release, el worker que está bloqueado en el acquire de la cola despertará
      Semaphore sem = fileSemaphores.get(f.getHash());
      if (sem != null) {
        sem.release();
      }
    });
  }

  private void enqueueRequest(Hash hash, Runnable dialTask) {
    // Al igual que en ScanFilesService, usamos LinkedBlockingQueue
    BlockingQueue<Runnable> queue = pendingQueues.computeIfAbsent(hash, k -> new LinkedBlockingQueue<>());
    queue.offer(dialTask); // Añadimos a la cola de espera
    log.debug("Task enqueued for hash {}. Queue size: {}", hash, queue.size());
  }

  private void initDownloadWorker(Hash hash) {

    workers.computeIfAbsent(hash, h -> {
      // Creamos un hilo virtual dedicado a consumir la cola de este archivo
      return Thread.ofVirtual().name("download-worker-" + hash.getValue()).start(() -> {
        BlockingQueue<Runnable> queue =
            pendingQueues.computeIfAbsent(hash, k -> new LinkedBlockingQueue<>());
        Semaphore semaphore = fileSemaphores.computeIfAbsent(hash, k -> new Semaphore(MAX_CONCURRENT_FRAGMENTS));

        try {
          while (downloadingFiles.containsKey(hash)) {
            // 1. Esperamos a tener un slot libre (como en tu ScanFilesService)
            semaphore.acquire();

            // 2. Sacamos la siguiente petición de la cola (bloqueante si está vacía)
            Runnable nextTask = queue.take();

            // 3. Ejecutamos el dialer
            nextTask.run();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      });
    });


  }

  private void stopAndCleanup(Hash hash) {
    // 1) Marca como no-activo (hará que el while termine si llega a evaluar la condición)
    downloadingFiles.remove(hash);

    // 2) Despierta al worker aunque esté bloqueado en take()/acquire()
    Thread t = workers.remove(hash);
    if (t != null) {
      t.interrupt();
    }

    // 3) Limpieza de estructuras (idempotente)
    BlockingQueue<Runnable> q = pendingQueues.remove(hash);
    if (q != null) q.clear();

    fileSemaphores.remove(hash);
  }

}
