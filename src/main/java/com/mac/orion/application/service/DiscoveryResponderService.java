package com.mac.orion.application.service;

import com.mac.orion.application.in.DiscoveryResponderUseCase;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Peer;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryResponderService implements DiscoveryResponderUseCase {

  @Value("${orion.p2p.limitK}")
  private Integer limitK;

  private final RoutingTable routingTable;

  @Override
  public Set<Peer> getClosestPeers(final String targetId) {

    final Set<Peer> candidates = routingTable.getAllPeers().stream()
        .peek(p -> log.debug("Peer: {} of routing table before xorDistanceCalculator", p.getId()))
        .collect(Collectors.toSet());

    final Set<Peer> closestCandidates = candidates.stream()
        .filter(peer -> !peer.getId().equals(targetId))
        .sorted(Comparator.comparing(
            peer -> determineDistanceXor(targetId.getBytes(), peer.getId().getBytes())))
        .limit(limitK)
        .collect(Collectors.toSet());

    log.info("Closest peers: [{}] for target: {}", closestCandidates.size(), targetId);

    log.info("routingTable of Host node: [{}]", routingTable.getHostNodeData().getId());
    routingTable.getAllPeers()
        .forEach(peer -> log.debug("Peer: {} of routing table to closest peers", peer.getId()));

    return closestCandidates;
  }

  private BigInteger determineDistanceXor(final byte[] first, final byte[] second) {
    int length = Math.min(first.length, second.length);
    byte[] xor = new byte[length];
    for (int i = 0; i < length; i++) {
      xor[i] = (byte) (first[i] ^ second[i]);
    }
    final BigInteger result = new BigInteger(1, xor);// 1 = positive
    log.debug("Resulting XOR: {}", result);

    return result;

  }

}
