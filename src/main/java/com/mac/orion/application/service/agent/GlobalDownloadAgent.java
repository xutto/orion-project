package com.mac.orion.application.service.agent;

import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.transfer.FileTransferData;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class GlobalDownloadAgent implements Agent {

  private final AtomicInteger limitKbSeg = new AtomicInteger(8 * 1024); // todo a config [download speed limit kb/s]
  private final AtomicLong nextFreeTimeNs = new AtomicLong(0L); // the initial value must be 0
  private final ConcurrentHashMap<Hash, Set<Integer>> ongoingFragmentsDownloads = new ConcurrentHashMap<>();


  public int getLimitKbSeg() {
    return limitKbSeg.get();
  }

  public void setLimitKbSeg(int limitKbSeg) {
    this.limitKbSeg.set(limitKbSeg);
  }


  public Set<Integer> getOngoingFragmentsDownloads(Hash hash) {
    return ongoingFragmentsDownloads.get(hash);
  }

  @Override
  public void audit(FileTransferData fileTransferData) {



    // state fragment on air transfer
    ongoingFragmentsDownloads
        .computeIfAbsent(fileTransferData.getHash(), h -> ConcurrentHashMap.newKeySet())
        .add(fileTransferData.getFileFragment().getIndex());

    // file transfer limit
    brakeTransfer(fileTransferData.getFileChunk().getData().length);

  }

  private void brakeTransfer(int bytes) {
    int kbps = limitKbSeg.get();
    if (kbps <= 0) return;
    long now = System.nanoTime();
    long costNs = (bytes * 1_000_000_000L) / (kbps * 1024L); // tiempo “presupuestado” para estos bytes


    // Reserva atómica de tu “slot” temporal
    long scheduledStart = nextFreeTimeNs.getAndUpdate(prev -> {
      long base = Math.max(prev, now);    // si vamos retrasados, encadena; si vamos holgados, empieza ahora
      return base + costNs;               // nuevo fin reservado
    });

    long start = Math.max(scheduledStart, now);
    long sleepNs = start - now;

    if (sleepNs > 10_000_000) { // 10ms
      try {
        // Con hilos virtuales, este sleep libera el carrier thread (no bloquea CPU)
        Thread.sleep(java.time.Duration.ofNanos(sleepNs));
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

  }
}
