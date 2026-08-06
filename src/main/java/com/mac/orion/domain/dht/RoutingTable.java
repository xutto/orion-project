package com.mac.orion.domain.dht;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.domain.model.HostNode;
import com.mac.orion.domain.model.Peer;
import java.util.Set;

public interface RoutingTable extends Publisher<Peer> {

  void addPeer(Peer peer);

  Peer getPeer(String id);

  void removePeer(Peer peer);

  void clear();

  int size();

  Set<Peer> getAllPeers();

  HostNode getHostNodeData();

  void setHostNodeData(HostNode newValue);
}
