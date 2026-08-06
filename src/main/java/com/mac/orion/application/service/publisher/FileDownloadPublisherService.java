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
public class FileDownloadPublisherService implements Publisher<File> {

  private final Map<OperationsType, List<Consumer<File>>> consumers = new HashMap<>();
  private final List<File> items = new LinkedList<>();

  public FileDownloadPublisherService() {

    Set<OperationsType> operations = new HashSet<>();
    operations.add(OperationsType.RETRIEVE);
    operations.add(OperationsType.RECLAIM);
    operations.add(OperationsType.REMOVE);
    operations.add(OperationsType.SAVE);
    operations.add(OperationsType.COMPLETE);
    operations.forEach(o -> consumers.put(o, new LinkedList<>()));
  }

  @Override
  public void subscribe(OperationsType operation, Consumer<File> consumer) {
    consumers.get(operation).add(consumer);

    if (operation == OperationsType.RECLAIM) {
      items.forEach(consumer);
    }
    if (operation == OperationsType.RETRIEVE) {
      items.forEach(consumer);
    }
    if (operation == OperationsType.SAVE) {
      items.forEach(consumer);
    }
    if (operation == OperationsType.COMPLETE) {
      items.forEach(consumer);
    }


  }

  /**
   * This publisher notifies when more file fragments are needed. When no more fragments are required,
   * it can be called with the REMOVE option to remove the file from the observer
   * @param operation RECLAIM, REMOVE
   * @param item The file to
   */
  @Override
  public void publish(OperationsType operation, File item) {

    switch (operation) {
      case RECLAIM, RETRIEVE, SAVE, COMPLETE -> {
        items.add(item);
        notify(operation, item);
      }
      case REMOVE -> {
        items.remove(item);
        notify(operation, item);
      }
    }

  }

  private void notify(OperationsType operation, File item) {
    consumers.get(operation)
        .forEach(consumer -> consumer.accept(item));
  }
}
