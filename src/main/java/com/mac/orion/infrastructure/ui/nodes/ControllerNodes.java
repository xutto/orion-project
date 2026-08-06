package com.mac.orion.infrastructure.ui.nodes;

import java.util.Collection;
import javafx.scene.Node;

public interface ControllerNodes {

    ControllerNodes addNode(Node node);

    void removeNode(Node node);

    Collection<Node> getNodes();

  /**
   * Retrieves a node from the collection by its identifier.
   * WARNING!. this identifier must be a unique value, and it is the id from CSS "id" attribute.
   * see {@link com.mac.orion.domain.share.NodesIdentifierConstants}
   *
   * @param id the identifier of the node to retrieve
   * @return the node with the specified identifier, or null if no such node exists
   */
  Node getNode(String id);

  /**
   * Retrieves a node with the specified identifier and casts it to the given class type.
   * WARNING!. this identifier must be a unique value, and it is the id from CSS "id" attribute.
   * see {@link com.mac.orion.domain.share.NodesIdentifierConstants}
   *
   * @param id the identifier of the node to retrieve
   * @param clazz the expected type of the node
   * @param <T> the type parameter corresponding to the expected class type
   * @return the node cast to the specified type, or throws an exception if the node is not found or cannot be cast
   * @throws ClassCastException if the node exists but is not of the specified type
   */
    <T extends Node> T getNode(String id, Class<T> clazz);
}
