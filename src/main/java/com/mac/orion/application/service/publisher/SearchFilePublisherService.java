package com.mac.orion.application.service.publisher;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.SearchResult;
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
public class SearchFilePublisherService implements Publisher<SearchResult> {

  private final Map<OperationsType, List<Consumer<SearchResult>>> consumers = new HashMap<>();
  private final List<SearchResult> items = new LinkedList<>();

  public SearchFilePublisherService() {
    Set<OperationsType> operations = new HashSet<>();
    operations.add(OperationsType.SAVE);
    operations.forEach(o -> consumers.put(o, new LinkedList<>()));
  }

  // TODO SE NECESITA UN METODO RESET PARA VACIAR LOS ITEMS

  @Override
  public void subscribe(OperationsType operation, Consumer<SearchResult> consumer) {

    consumers.get(operation).add(consumer);
    if (operation == OperationsType.SAVE) {
      items.forEach(consumer); // re-emit only for SAVE operations
    }
    log.info("Subscribed to operation: {} with consumer: {}", operation, consumer);

  }

  @Override
  public void publish(OperationsType operation, SearchResult searchResult) {

    if (operation == OperationsType.SAVE) {
      items.add(searchResult);
      notify(operation, searchResult);
    }

  }

  private void notify(OperationsType operation, SearchResult searchResult) {
    consumers.get(operation)
        .forEach(consumer -> consumer.accept(searchResult));
  }
}
