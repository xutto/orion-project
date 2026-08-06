package com.mac.orion.application.service.publisher;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


@Slf4j
@Component
public class DownloadingPublisherService implements Publisher<Transfer> {

  private final Map<OperationsType, List<Consumer<Transfer>>> consumers = new HashMap<>();
  private final List<Transfer> items = new LinkedList<>();

  public DownloadingPublisherService() {

    Set<OperationsType> operations = new HashSet<>();
    operations.add(OperationsType.DOWNLOADING);
    operations.add(OperationsType.REMOVE);
    operations.forEach(o -> consumers.put(o, new LinkedList<>()));

  }

  // todo el que se suscribe al bitset, tiene que setear el progreso, en el hilo de jfx
  @Override
  public void subscribe(OperationsType operation, Consumer<Transfer> consumer) {
    consumers.get(operation).add(consumer);

    if (operation == OperationsType.DOWNLOADING) {
      items.forEach(consumer::accept);
    }

    log.info("Subscribed to operation: {}", operation);

  }

  // todo publicar el bitset, lo publicará un task con intervalos de 1s (configurable)
  @Override
  public void publish(OperationsType operation, Transfer item) {

    switch (operation) {
      // "Actualiza si existe, si no lo crea": put simplemente reemplaza la instancia
      case DOWNLOADING, REMOVE -> {
        items.add(item);
        notify(operation, item);
      }
    }
  }

  private void notify(OperationsType operation, Transfer item) {
    consumers.get(operation)
        .forEach(consumer -> consumer.accept(item));
  }
}
