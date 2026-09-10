package com.mac.orion.infrastructure.ui.command;

import com.google.common.net.InetAddresses;
import com.mac.orion.application.in.UpdateRoutingTableUseCase;
import com.mac.orion.domain.model.Address;
import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.ui.creation.SettingsAlertsCreator;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import io.libp2p.core.PeerId;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_ADD_BOOTSTRAP_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_ID_FIELD;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_IP_FIELD;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_PORT_FIELD;

/**
 * Adds a bootstrap contact (ip/port/id) to the in-memory routing table from the Settings panel.
 * Reuses the existing non-persistent {@link UpdateRoutingTableUseCase#addGuestPeerToRoutingTable(Peer)};
 * the peer is then dialed automatically by {@code PeerDiscoveryScheduler} (<= 15 s).
 * <p>
 * Validation is strict (DECISIÓN-02, option 2): IPv4 + port 1-65535 + peer ID. The peer ID is checked
 * with the SAME {@code PeerId.fromBase58} the dialer uses at runtime ({@code KadDialerAdapter}), so an
 * id accepted here cannot fail the dial for a malformed format.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AddBootstrapCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final UpdateRoutingTableUseCase updateRoutingTableUseCase;
  private final SettingsAlertsCreator alertsCreator;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(settingsControllerNodes.getNode(SETTINGS_ADD_BOOTSTRAP_BUTTON)));
  }

  @Override
  public void execute(Event event) {
    final String ip = readField(SETTINGS_BOOTSTRAP_IP_FIELD);
    final String rawPort = readField(SETTINGS_BOOTSTRAP_PORT_FIELD);
    final String peerId = readField(SETTINGS_BOOTSTRAP_ID_FIELD);

    final Integer port = parsePort(rawPort);
    if (!isValidIpv4(ip) || port == null || !isValidPeerId(peerId)) {
      log.warn("Invalid bootstrap contact, not added (ip={}, port={}, id={})", ip, rawPort, peerId);
      alertsCreator.createInvalidBootstrapAlert().showAndWait();
      return;
    }

    final Address address = Address.builder()
        .type("ip4")
        .transmission("tcp")
        .port(port)
        .ip(ip)
        .build();
    final Peer peer = Peer.builder()
        .id(peerId)
        .address(Set.of(address))
        .build();

    updateRoutingTableUseCase.addGuestPeerToRoutingTable(peer);
    log.info("Bootstrap peer added to the routing table: {}:{} (id={})", ip, port, peerId);

    clearField(SETTINGS_BOOTSTRAP_IP_FIELD);
    clearField(SETTINGS_BOOTSTRAP_PORT_FIELD);
    clearField(SETTINGS_BOOTSTRAP_ID_FIELD);
  }

  private String readField(String id) {
    final TextField field = settingsControllerNodes.getNode(id, TextField.class);
    final String text = field.getText();
    return text == null ? "" : text.trim();
  }

  private void clearField(String id) {
    settingsControllerNodes.getNode(id, TextField.class).setText("");
  }

  /**
   * Accepts a syntactically valid IPv4 address only (empty and IPv6 are rejected).
   */
  private boolean isValidIpv4(String ip) {
    if (ip.isEmpty() || ip.contains(":")) {
      return false;
    }
    return InetAddresses.isInetAddress(ip);
  }

  private Integer parsePort(String rawPort) {
    try {
      final int port = Integer.parseInt(rawPort);
      return (port >= 1 && port <= 65535) ? port : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Validated with the same call the dialer makes at runtime (KadDialerAdapter).
   */
  private boolean isValidPeerId(String peerId) {
    if (peerId.isEmpty()) {
      return false;
    }
    try {
      PeerId.fromBase58(peerId);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
