package com.mac.orion.domain.dht;

import com.mac.orion.domain.model.HostNode;
import com.mac.orion.domain.model.Peer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutingDataHashTable implements RoutingTable {

  //  private final List<Consumer<Peer>> subscribers = new ArrayList<>();
  private final Map<OperationsType, List<Consumer<Peer>>> consumers = new HashMap<>();
  private final ConcurrentHashMap<String, Peer> routingTable = new ConcurrentHashMap<>();
  private final AtomicReference<HostNode> hostNodeData;
  private static RoutingDataHashTable instance;

  private RoutingDataHashTable(HostNode hostNodeData, Set<OperationsType> operations) {
    log.info("RoutingDataHashTable created");
    this.hostNodeData = new AtomicReference<>(hostNodeData);
    operations.forEach(o -> consumers.put(o, new LinkedList<>()));
  }

  public static RoutingDataHashTable create(HostNode hostNode, Set<OperationsType> operations) {
    if (instance == null) {
      instance = new RoutingDataHashTable(hostNode, operations);
    }
    return instance;
  }

  @Override
  public void addPeer(Peer peer) {
    log.debug("PROD routing-table - addPeer: {}", peer);
    log.debug("PROD routing-table - PEERS: [{}]", routingTable.size());
    Optional.ofNullable(peer)
        .ifPresent(p -> routingTable.put(p.getId(),
            p)); // todo operation must be same in "publish()" method
  }

  @Override
  public Peer getPeer(String id) {
    return routingTable.get(id);
  }

  @Override
  public void removePeer(Peer peer) {
    log.info("PROD routing-table - removePeer: {}", peer);
    routingTable.remove(peer.getId());
  }

  @Override
  public void clear() {
    routingTable.clear();
  }

  @Override
  public int size() {
    return routingTable.size();
  }

  @Override
  public Set<Peer> getAllPeers() {
    return new HashSet<>(routingTable.values());
  }

  @Override
  public HostNode getHostNodeData() {
    return hostNodeData.get();
  }

  @Override
  public void setHostNodeData(HostNode newValue) {
    hostNodeData.set(newValue);
  }

  @Override
  public void subscribe(OperationsType operation, Consumer<Peer> consumer) {
    log.info("getAllPeers on suscribe: {}", getAllPeers().size());
    consumers.get(operation).add(consumer);
    consumers.get(operation)
        .forEach(c -> getAllPeers().forEach(consumer));
  }

  @Override
  public void publish(OperationsType operation, Peer peer) {
    log.debug("Published peer before: {}", peer);
    switch (operation) {
      case SAVE -> {
        log.debug("Save peer: {}", peer);
        addPeer(peer);
        notify(operation, peer);
      }
      case REMOVE -> {
        log.debug("Remove peer: {}", peer);
        removePeer(peer);
        notify(operation, peer);
      }
    }
  }

  private void notify(OperationsType operation, Peer peer) {
    consumers.get(operation)
        .forEach(consumer -> {
          consumer.accept(peer);
          log.debug("Published peer: {}", peer);
        });
  }


}
