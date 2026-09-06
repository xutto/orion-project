package com.mac.orion.infrastructure.ui.connection;

import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import com.mac.orion.infrastructure.util.PublicIpResolver;
import io.libp2p.core.Host;
import javafx.application.Platform;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_LABEL_IP;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionInfoProvider {

  public static final String IP_DEFAULT_LOCALHOST = "127.0.0.1";

  private static final ExecutorService publicIpExecutor = Executors.newVirtualThreadPerTaskExecutor();

  private final Host hostNode;
  private final NodeConfigUseCase nodeConfigUseCase;
  private final ControllerNodes settingsControllerNodes;
  private final PublicIpResolver publicIpResolver;

  /** Public IP resolved at startup (null until the first successful resolution). */
  private volatile String resolvedPublicIp;

  /**
   * Public IP resolved via https://api.ipify.org, or fallback 127.0.0.1.
   *
   * @return the local ip address as string
   */
  public String getLocalIp() {
    final String ip = resolvedPublicIp;
    return ip != null ? ip : IP_DEFAULT_LOCALHOST;
  }

  /**
   * @return the p2p client port from the NODE_CONFIG row (String)
   */
  public String getPort() {
    return String.valueOf(nodeConfigUseCase.getPort());
  }

  /**
   * @return the own host peer id (base58)
   */
  public String getPeerId() {
    return hostNode.getPeerId().toString();
  }

  /**
   * Resolves the public IP in a background thread (https://api.ipify.org) and, when it
   * succeeds, updates the IP label of the connection panel. On failure the label keeps
   * the 127.0.0.1 fallback.
   */
  public void refreshPublicIp() {
    publicIpExecutor.submit(() -> {
      final String ip = publicIpResolver.resolve();
      if (ip == null) {
        return; // failure already logged by the resolver
      }
      this.resolvedPublicIp = ip;
      updateIpLabel();
    });
  }

  /**
   * Refreshes the IP label of the connection panel.
   * Defensive: it may be invoked from a non-FX thread before the FXML has been loaded.
   */
  private void updateIpLabel() {
    try {
      Platform.runLater(() -> {
        try {
          final Label ipLabel = settingsControllerNodes.getNode(SETTINGS_LABEL_IP, Label.class);
          ipLabel.setText(getLocalIp());
        } catch (Exception e) {
          log.debug("IP label not available yet (FXML not loaded?), skipping refresh", e);
        }
      });
    } catch (IllegalStateException e) {
      log.debug("JavaFX toolkit not initialized (NO-UI mode?), skipping IP label refresh", e);
    }
  }
}
