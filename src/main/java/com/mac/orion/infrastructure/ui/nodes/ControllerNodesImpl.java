package com.mac.orion.infrastructure.ui.nodes;

import java.util.Collection;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ControllerNodesImpl implements ControllerNodes{

  private final Collection<Node> nodes;

  @Override
  public ControllerNodes addNode(Node node) {
    this.nodes.add(node);
    return this;
  }

  @Override
  public void removeNode(Node node) {
    this.nodes.remove(node);
  }

  @Override
  public Collection<Node> getNodes() {
    return this.nodes;
  }

  @Override
  public Node getNode(String id) {
    try {
      return nodes.stream()
          .filter(n -> n.getId().equals(id))
          .findFirst()
          .orElseThrow(RuntimeException::new);
    } catch (Exception e) {
      log.error("Node: [{}] maybe not exists or there are some problems to retrieve node", id, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T extends Node> T getNode(String id, Class<T> clazz) {
    final Node node = getNode(id);
    if (clazz.isAssignableFrom(node.getClass())) {
      return clazz.cast(node);
    }
    throw new ClassCastException("El nodo no es del tipo esperado: " + clazz.getName());
  }
}
