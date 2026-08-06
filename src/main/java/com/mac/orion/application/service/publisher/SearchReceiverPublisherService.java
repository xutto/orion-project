package com.mac.orion.application.service.publisher;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.SearchResource;
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
public class SearchReceiverPublisherService implements Publisher<SearchResource> {

  private final Map<OperationsType, List<Consumer<SearchResource>>> consumers = new HashMap<>();
  private final List<SearchResource> items = new LinkedList<>();

  public SearchReceiverPublisherService() {
    Set<OperationsType> operations = new HashSet<>();
    operations.add(OperationsType.RECEIVER);
    operations.forEach(o -> consumers.put(o, new LinkedList<>()));
  }

  @Override
  public void subscribe(OperationsType operation, Consumer<SearchResource> consumer) {

    consumers.get(operation).add(consumer);
    if (operation == OperationsType.RECEIVER) {
      items.forEach(consumer); // re-emit only for RECEIVER operations
    }
    log.info("Subscribed to operation: {} with consumer: {}", operation, consumer);
  }

  @Override
  public void publish(OperationsType operation, SearchResource item) {

    if (operation == OperationsType.RECEIVER){
      log.info("Add searchResource: {}", item);
      items.add(item);
      notify(operation, item);
    }

  }

  private void notify(OperationsType operation, SearchResource item) {
    consumers.get(operation)
        .forEach(consumer -> consumer.accept(item));
  }
}
