package com.mac.orion.infrastructure.ui.creation;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_TOOLTIP_BOOTSTRAP;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_TOOLTIP_CONNECTION;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_TOOLTIP_LIMIT_K;

@Getter
public class TooltipsCreator implements Creator {

  private List<Tooltip> settingsTooltipHelpers;

  public static final String TOOLTIP_CONNECTION_HELPER = "This section displays your current connection details. You can change the port using the input field and clicking 'Change port'. The ID is unique to your node on the network.";
  public static final String TOOLTIP_BOOSTRAP_HELPER = "Bootstrap nodes are entry points to the P2P network. Add new nodes by providing their IP, Port, and ID. These nodes help you discover and connect to other peers.";
  public static final String TOOLTIP_LIMIT_K_HELPER = "The LimitK parameter affects network performance. A higher value allows more connections but consumes additional resources. The default is 20; adjust it according to system capacity.";

  @Override
  public void init() {

    final List<Tooltip> settingsTooltipHelpers = new ArrayList<>();


    final Tooltip connectionTooltip = createStyledTooltip(
        TOOLTIP_CONNECTION_HELPER, SETTINGS_TOOLTIP_CONNECTION
    );

    final Tooltip bootstrapTooltip = createStyledTooltip(
        TOOLTIP_BOOSTRAP_HELPER, SETTINGS_TOOLTIP_BOOTSTRAP
    );

    final Tooltip limitKTooltip = createStyledTooltip(
        TOOLTIP_LIMIT_K_HELPER, SETTINGS_TOOLTIP_LIMIT_K

    );

    settingsTooltipHelpers.add(connectionTooltip);
    settingsTooltipHelpers.add(bootstrapTooltip);
    settingsTooltipHelpers.add(limitKTooltip);

    this.settingsTooltipHelpers = settingsTooltipHelpers;
  }

  private Tooltip createStyledTooltip(String text, String id) {
    Tooltip tooltip = new Tooltip();
    tooltip.setText(text);
    tooltip.setShowDelay(Duration.ZERO);
    tooltip.setWrapText(true);
    tooltip.setHideOnEscape(true);
    tooltip.setStyle("-fx-background-color: #23262e; -fx-text-fill: #cfcfcf; -fx-font-family: \"Roboto\"; -fx-font-size: 12px;");
    tooltip.setId(id);
    tooltip.setMaxHeight(200);
    tooltip.setMaxWidth(300);
    tooltip.setAutoHide(true);
    return tooltip;
  }

}
