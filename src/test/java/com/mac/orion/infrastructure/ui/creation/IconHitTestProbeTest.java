package com.mac.orion.infrastructure.ui.creation;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probe (regression) for the bug "no puedo eliminar bootstraps desde la UI".
 *
 * <p>Scenario: a row icon is a StackPane that carries the PROPERTIES_BOOTSTRAP payload (as
 * {@link BootstrapListCreator} wires it) with an SVGPath glyph as its child, and the
 * {@code MOUSE_CLICKED} handler is on the StackPane. A real click hits the deepest pickable
 * node (the glyph).
 *
 * <p>Empirical result of this headless dispatch (event fired from the hit node, handled by the
 * ancestor, no Scene):
 * <ul>
 *   <li>{@code event.getSource()} is the StackPane (the node handling the event),</li>
 *   <li>{@code event.getTarget()} is the SVGPath glyph (the hit node),</li>
 *   <li>the payload is present on the source, absent on the target, and is found by walking up
 *       from either node.</li>
 * </ul>
 *
 * <p>In the real glass dispatch (click inside a live Scene) the source is expected to be the hit
 * glyph; a source-only payload read (as {@code BootstrapDeleteCommand} /
 * {@code BootstrapCopyCommand} do) would then find nothing — the reported bug. A headless test
 * cannot reproduce the real pick/dispatch, so this test locks in the simulated semantics and
 * documents the discrepancy: the commands must resolve the payload from source, target and the
 * ancestor chains, not from the source alone.
 */
class IconHitTestProbeTest {

  private static final String PAYLOAD_KEY = "PROBEPAYLOAD";
  private static final String PAYLOAD_VALUE = "present-on-StackPane";

  private static final String COPY_SVG =
      "M192 0c-35.3 0-64 28.7-64 64l0 256c0 35.3 28.7 64 64 64l192 0c35.3 0 64-28.7 64-64l0-200.6"
          + "c0-17.4-7.1-34.1-19.7-46.2L370.6 17.8C358.7 6.4 342.8 0 326.3 0L192 0zM64 128c-35.3 0"
          + "-64 28.7-64 64L0 448c0 35.3 0 64 64 64l192 0c35.3 0 64-28.7 64-64l0-16-64 0 0 16"
          + "-192 0 0-256 16 0 0-64-16 0z";

  private record HandlerObservation(Node source,
                                    Node target,
                                    Object payloadAtSource,
                                    Object payloadAtTarget,
                                    Object payloadViaWalkUpFromSource,
                                    Object payloadViaWalkUpFromTarget) {
  }

  private static Object walkUpForPayload(Node start) {
    Node walk = start;
    while (walk != null) {
      final Object value = walk.getProperties().get(PAYLOAD_KEY);
      if (value != null) {
        return value;
      }
      walk = walk.getParent();
    }
    return null;
  }

  @Test
  void handlerSeesPaneAsSourceAndGlyphAsTarget() {
    // Guard: in the full suite another test may have started the toolkit already.
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException alreadyInitialized) {
      // toolkit already up in this JVM — fine
    }

    final SVGPath icon = new SVGPath();
    icon.setContent(COPY_SVG);
    final StackPane pane = new StackPane(icon);
    pane.setId("bootstrap-copy-icon");
    pane.setMinSize(32, 45);
    pane.setPrefSize(32, 45);
    pane.setMaxSize(32, 45);
    pane.getProperties().put(PAYLOAD_KEY, PAYLOAD_VALUE);

    final AtomicReference<HandlerObservation> observation = new AtomicReference<>();
    pane.setOnMouseClicked(event -> {
      final Node source = event.getSource() instanceof Node n ? n : null;
      final Node target = event.getTarget() instanceof Node n ? n : null;
      observation.set(new HandlerObservation(
          source,
          target,
          source == null ? null : source.getProperties().get(PAYLOAD_KEY),
          target == null ? null : target.getProperties().get(PAYLOAD_KEY),
          source == null ? null : walkUpForPayload(source),
          target == null ? null : walkUpForPayload(target)));
    });

    // Fire the click from the hit node (the glyph), as glass does after a pick.
    final MouseEvent click = new MouseEvent(icon, icon, MouseEvent.MOUSE_CLICKED,
        16, 22, 16, 22, MouseButton.PRIMARY, 1,
        false, false, false, false, true, false, false, false, false, false, null);
    Event.fireEvent(icon, click);

    final HandlerObservation obs = observation.get();
    assertNotNull(obs, "the StackPane handler must have been invoked");
    assertSame(pane, obs.source(), "empirical: the source is the node handling the event (StackPane)");
    assertSame(icon, obs.target(), "empirical: the target is the hit node (SVGPath glyph)");
    assertTrue(obs.source != obs.target, "source and target are different nodes in this dispatch");
    assertEquals(PAYLOAD_VALUE, obs.payloadAtSource(), "the payload lives on the source (StackPane)");
    assertNull(obs.payloadAtTarget(), "the payload is NOT on the target (glyph)");
    assertEquals(PAYLOAD_VALUE, obs.payloadViaWalkUpFromSource(),
        "walking up from the source finds the payload");
    assertEquals(PAYLOAD_VALUE, obs.payloadViaWalkUpFromTarget(),
        "walking up from the target (glyph -> StackPane) also finds the payload");
  }
}
