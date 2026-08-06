package com.mac.orion.application.service.publisher;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FilePublisherService implements Publisher<File> {

  //  private final List<Consumer<File>> subscribers = new ArrayList<>();
  private final Map<OperationsType, List<Consumer<File>>> consumers = new HashMap<>();
  private final List<File> items = new LinkedList<>();

  public FilePublisherService() {
    Set<OperationsType> operations = new HashSet<>();
    operations.add(OperationsType.SAVE);
    operations.add(OperationsType.REMOVE);
    operations.forEach(o -> consumers.put(o, new LinkedList<>()));
  }

  /**
   * Subscribes a consumer to receive notifications for specific file operations. When a consumer
   * subscribes to SAVE operations, it will immediately receive all existing files. The subscribed
   * consumer will process file operations based on the specified operation type (SAVE/REMOVE).
   *
   * @param operation The type of operation to subscribe to (SAVE or REMOVE)
   * @param consumer  The consumer function that will handle the file operations
   */
  @Override
  public void subscribe(OperationsType operation, Consumer<File> consumer) {

    consumers.get(operation).add(consumer);
    if (operation == OperationsType.SAVE) {
      items.forEach(consumer); // re-emit only for SAVE operations
    }
    log.info("Subscribed to operation: {} with consumer: {}", operation, consumer);
  }


  @Override
  public void publish(OperationsType operation, File file) {

    switch (operation) {
      case SAVE -> {
        log.debug("Add file: {}", file);
        items.add(file);
        notify(operation, file);
      }
      case REMOVE -> {
        log.debug("Remove file: {}", file);
        items.remove(file);
        notify(operation, file);
      }
    }
  }

  private void notify(OperationsType operation, File item) {
    consumers.get(operation)
        .forEach(consumer -> consumer.accept(item));
  }

}
