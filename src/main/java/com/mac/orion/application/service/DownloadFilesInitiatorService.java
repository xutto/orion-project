package com.mac.orion.application.service;

import com.mac.orion.application.in.DownloadFilesInitiatorUseCase;
import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.scheduler.DownloadingCheckDataScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.mac.orion.domain.dht.OperationsType.RECLAIM;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadFilesInitiatorService implements DownloadFilesInitiatorUseCase {

  private final FileAllocatorService fileAllocatorService;
  private final Publisher<File> fileDownloadPublisherService;
  private final DownloadingCheckDataScheduler downloadingCheckDataScheduler;

  @Override
  public void downloadFileInitiatorProcess(final File file) {

    if (file == null) {
      log.error("File not found");
      return;
    }

    try {
      // step: create the file
      log.info("Starting download process for file: [{}] - hash [{}]", file.getNames().stream().findFirst(), file.getHash());
      final File allocatedFile = fileAllocatorService.allocateFilePhysical(file);

      if (allocatedFile == null) {
        log.error("File not allocated, cannot create filesystem for file: [{}] - hash [{}]", file.getNames().stream().findFirst(), file.getHash());
        return;
      }

      // step: create sidecar
      log.info("Creating sidecar for file: [{}] - hash [{}]", allocatedFile.getNames().stream().findFirst(), allocatedFile.getHash());
      final boolean sidecarCreated = fileAllocatorService.allocateSidecar(allocatedFile);

      if (!sidecarCreated) {
        log.error("Sidecar not allocated, cannot create sidecar for file: [{}] - hash [{}]", allocatedFile.getNames().stream().findFirst(), allocatedFile.getHash());
        return;
      }

      // step: publish download event (RECLAIM)
      log.info("Publishing RECLAIM event for file: [{}] - hash [{}]", allocatedFile.getNames().stream().findFirst(), allocatedFile.getHash());
      fileDownloadPublisherService.publish(RECLAIM, allocatedFile);

      // step: init downloading check data scheduler
      log.info("Starting downloading check data scheduler for file: [{}] - hash [{}]", allocatedFile.getNames().stream().findFirst(), allocatedFile.getHash());
      downloadingCheckDataScheduler.startProcess(allocatedFile.getHash());

      log.info("File download initiated successfully for file: [{}] - hash [{}]", allocatedFile.getNames().stream().findFirst(), allocatedFile.getHash());
    } catch (IOException e) {
      log.error("Error during file download initiation", e);
      throw new RuntimeException(e);
    }

//    Optional.ofNullable(file)
//        .map(fileAllocatorService::allocateFilePhysical)
//        .filter(f -> {
//          try {
//            return fileAllocatorService.allocateSidecar(f);
//          } catch (IOException e) {
//            log.error("Error allocating sidecar", e);
//            throw new RuntimeException(e);
//          }
//        })
////        .map(filesPort::save) todo se salva en BD aquí?? , ahora se salva en el metodo  allocateFilePhysical
//        .ifPresentOrElse(f -> {
//              fileDownloadPublisherService.publish(RECLAIM, f);
//              downloadingCheckDataScheduler.startProcess(f);
//            },
//            () -> log.error("File not allocated")); //todo mejorar este error

  }

  // todo CAN IMPLEMENT THIS METHOD, interesting...
  private void handleFailure(File file, String reason) {
    log.error("DOWNLOAD_FAILED: {} - Hash: {}", reason, (file.getHash() != null ? file.getHash() : "N/A"));
    // Aquí podrías actualizar el estado del archivo en la BD a ERROR si fuera necesario
    // filesPort.save(file.toBuilder().status(Status.ERROR).build());
  }

}
