package com.mac.orion.application.service;

import static com.mac.orion.domain.dht.OperationsType.SAVE;

import com.mac.orion.application.commons.FileProcessUtils;
import com.mac.orion.application.in.Publisher;
import com.mac.orion.application.in.ScanFilesUseCase;
import com.mac.orion.application.out.FilesPort;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Status;
import com.mac.orion.domain.model.settings.Settings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScanFilesService implements ScanFilesUseCase {

  //  private static final ExecutorService filesScanExecutor = Executors.newVirtualThreadPerTaskExecutor();
  private static final ExecutorService md5FilesProcessExecutor = Executors.newVirtualThreadPerTaskExecutor();
  private static final ExecutorService saveFilesExecutor = Executors.newVirtualThreadPerTaskExecutor();
  private static final Semaphore md5FilesProcessSemaphore = new Semaphore(
      20); // todo a config [md5 scan process simultaneous]
  private static final Semaphore saveFilesSemaphore = new Semaphore(
      4); // todo a config [max files to save simultaneous]
  private final BlockingQueue<File> queueFileListToSave = new LinkedBlockingQueue<>(
      10); // todo a config [max files in queue to save simultaneous]
  private final Set<String> filesInProcess = ConcurrentHashMap.newKeySet();
  volatile boolean filesAreProcessing = true;

  private final FilesPort filesRepository;
  private final Publisher<File> filePublisherService;
  private final Settings settings;

  @Override
  public void scanFolders() {
    log.info("Start scanning folders: {}", settings.getDirectories().scan());

    settings.getDirectories().scan().forEach(this::scanFiles);

  }

  @Override
  public void scanFiles(final String folder) {

    final Path startPath = Paths.get(folder);

    // todo tener en cuenta el tamaño de este mapa, podría haber millones de ficheros
    final Map<String, File> filesFromDB = filesRepository.findAll().stream()
        .collect(Collectors.toMap(File::getPath, Function.identity(), (f1, f2) -> f1));

    final List<Path> allPaths = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(startPath)) {
      allPaths.addAll(paths.filter(Files::isRegularFile).toList());
    } catch (NoSuchFileException e) {
      log.warn("Folder files not found: [{}]", e.getMessage());
    } catch (IOException e) {
      log.warn("error when walk files", e);
    }

    final CountDownLatch md5Latch = new CountDownLatch(allPaths.size());
    allPaths.forEach(path ->
            md5FilesProcessExecutor.execute(() -> {
              try {
                Thread.sleep(100); // todo solucion temporal al bug del cuelgue de los hilos
                md5FilesProcessSemaphore.acquireUninterruptibly();
                this.processFile(path, filesFromDB);
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              } finally {
                md5FilesProcessSemaphore.release();
                md5Latch.countDown();

//              md5FilesProcessExecutor.shutdown();
                log.debug("MD5 FINISHED: {}", path.getFileName());
              }
            })
    );

    saveFilesExecutor.execute(() -> {
      try {
        final List<File> blockToSave = new ArrayList<>();
        saveFilesSemaphore.acquireUninterruptibly();
        while (filesAreProcessing) {
          blockToSave.clear();
          queueFileListToSave.drainTo(blockToSave, 10);

          if (!blockToSave.isEmpty()) {
            saveFiles(blockToSave);
          } else {
            // TODO implementar un mecanismo de fin de proceso

            if (md5Latch.getCount() == 0L && blockToSave.isEmpty()) {
              log.debug("blockToSave is empty");
              break;
            }

          }

        }
      } catch (Exception e) {
        log.warn("ERROR SAVING FILES", e);
      } finally {
        saveFilesSemaphore.release();
//        saveFilesExecutor.shutdown();
      }
    });

//    try {
//      final boolean md5ProcessFinished = md5FilesProcessExecutor.awaitTermination(1,
//          TimeUnit.HOURS);
//      log.info("MD5 FINISHED: {}", md5ProcessFinished);
//      filesAreProcessing = false;
//      final boolean saveFilesProcessorFiinished = saveFilesExecutor.awaitTermination(1,
//          TimeUnit.HOURS);
//      log.info("SAVE FILES FINISHED: {}", saveFilesProcessorFiinished);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }
  }

  private void processFile(Path path, Map<String, File> filesFromDB) {

    // Clause guard if the file is already processing
    if (filesInProcess.contains(path.toAbsolutePath().toString())) {
      log.debug("File is already processing: {}", path.getFileName());
      return;
    }

    try {
      final String filename = path.getFileName().toString();
      log.debug("File to process: {}", path);
      long startTime = System.nanoTime();

      final Optional<File> optionalFile = checkDBFile(path, filesFromDB);
      final File fileProcessed;
      if (optionalFile.isPresent()) {
        fileProcessed = optionalFile.get();
        log.debug("File already processed: {}", fileProcessed.getNames().stream().findFirst());
      } else {

        filesInProcess.add(path.toAbsolutePath().toString());

        // md5 hash
        final String md5String = FileProcessUtils.checksumMD5String(path);
        final Hash hashMD5 = Hash.builder().value(md5String).build();

        // names
        final Set<String> names = new HashSet<>();
        names.add(filename);

        // build file processed
        fileProcessed = File.builder()
            .names(names)
            .path(path.toAbsolutePath().toString())
            .size(Files.size(path))
            .hash(hashMD5)
            .peers(new HashMap<>())
            .status(Status.STORED) // TODO check default status file
            .build();
        queueFileListToSave.put(fileProcessed);
        long duration = System.nanoTime() - startTime;  // Calcular la duración
        log.info("File processed: {} (MD5 calculated in {} ms)",
            fileProcessed.getNames().stream().findFirst(),
            duration / 1_000_000);
      }

      // publish file always
      filePublisherService.publish(SAVE, fileProcessed);

    } catch (IOException e) {
      log.warn("error when retrieve file size", e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      filesInProcess.remove(path.toAbsolutePath().toString());
    }
  }

  private Optional<File> checkDBFile(Path path, Map<String, File> filesFromDB) {
    // Se comprueba que el path de filesystem efectivamente existe en la base de datos
    return Optional.ofNullable(filesFromDB.get(path.toAbsolutePath().toString()));
  }


  private void saveFiles(List<File> files) {
    files.forEach(e -> {
      log.debug("THE FILE TO SAVE {}", e);

      // publish file
//      filePublisherService.publish(SAVE, e);
    });
    filesRepository.saveAll(files);
  }
}
