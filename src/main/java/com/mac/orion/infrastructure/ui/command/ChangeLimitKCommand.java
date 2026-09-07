package com.mac.orion.infrastructure.ui.command;

import com.mac.orion.application.out.NodeConfigUseCase;
import com.mac.orion.infrastructure.ui.creation.SettingsAlertsCreator;
import com.mac.orion.infrastructure.ui.events.EventType;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_APPLY_LIMIT_K_BUTTON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_LIMIT_K_SPINNER;

/**
 * Applies the LimitK value shown in the settings spinner (set via the spinner arrows
 * or typed in its editor) to the database. Takes effect on the next discovery reply —
 * no restart (request [001], point 1).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ChangeLimitKCommand implements Command<Event> {

  private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

  private final ControllerNodes settingsControllerNodes;
  private final NodeConfigUseCase nodeConfigUseCase;
  private final SettingsAlertsCreator alertsCreator;

  @Override
  public Map<EventType, List<Node>> getCompatibilities() {
    return this.compatibilities;
  }

  @Override
  public void configureCompatibility() {
    compatibilities.put(EventType.MOUSE_CLICKED,
        List.of(settingsControllerNodes.getNode(SETTINGS_APPLY_LIMIT_K_BUTTON)));
  }

  @Override
  public void execute(Event event) {
    final Spinner<Integer> spinner = settingsControllerNodes.getNode(SETTINGS_LIMIT_K_SPINNER, Spinner.class);
    // Deterministic read: the editor text reflects both the arrows and typed input,
    // regardless of commitEdit (same raw->parse pattern as ChangePortCommand).
    final String raw = spinner.getEditor().getText() == null ? "" : spinner.getEditor().getText().trim();

    Integer newLimitK = null;
    try {
      newLimitK = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      // not a valid integer, keep null
    }
    if (newLimitK == null || newLimitK < 1 || newLimitK > 100) {
      log.warn("Invalid LimitK input: '{}'", raw);
      alertsCreator.createInvalidLimitKAlert().showAndWait();
      return;
    }
    final int current = nodeConfigUseCase.getLimitK();
    if (newLimitK == current) {
      log.info("LimitK unchanged ({}), nothing to do", newLimitK);
      return;
    }

    nodeConfigUseCase.updateLimitK(newLimitK);
    log.info("LimitK applied from settings: {} -> {} (effective on the next discovery reply, no restart)",
        current, newLimitK);
  }
}
