package com.mac.orion.domain.dht;

import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Peer;

import java.util.Set;

public interface FileRoutingTable {

  void addFile(File file);

  Set<File> getData();

  Set<Peer> getPeersByfile(Hash hash);

  void removePeerFromFile(Hash fileHash, Peer peer);

  void clear();

  int peersByFile(Hash hash);

  Set<File> getFilesByHashes(Set<Hash> hashes);

}
