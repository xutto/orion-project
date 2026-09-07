package com.mac.orion.application.service;

import com.mac.orion.application.in.DiscoveryResponderUseCase;
import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.domain.dht.RoutingTable;
import com.mac.orion.domain.model.Peer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryResponderService implements DiscoveryResponderUseCase {

  private final RoutingTable routingTable;
  private final NodeConfigUseCase nodeConfigUseCase;

  @Override
  public Set<Peer> getClosestPeers(final String targetId) {

    final Set<Peer> candidates = routingTable.getAllPeers().stream()
        .peek(p -> log.debug("Peer: {} of routing table before xorDistanceCalculator", p.getId()))
        .collect(Collectors.toSet());

    // Read per query: a value applied from settings takes effect on the next reply, no restart.
    final int limitK = nodeConfigUseCase.getLimitK();

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
