package com.mac.orion.infrastructure.ui.creation;

import com.mac.orion.application.out.BootstrapUseCase;
import com.mac.orion.domain.model.settings.Bootstrap;
import com.mac.orion.infrastructure.ui.command.BootstrapCopyCommand;
import com.mac.orion.infrastructure.ui.command.BootstrapDeleteCommand;
import com.mac.orion.infrastructure.ui.events.EventsAssemblerImpl;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodesImpl;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_DELETE_ICON;
import static com.mac.orion.domain.share.NodesIdentifierConstants.SETTINGS_BOOTSTRAP_LIST_CONTAINER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test (bug: "no puedo eliminar bootstraps desde la UI"): builds the REAL per-row delete
 * chain — real BootstrapListCreator + real BootstrapDeleteCommand + real EventsAssemblerImpl + real
 * ControllerNodesImpl — with an in-memory BootstrapUseCase and a stubbed confirmation dialog (always
 * DELETE_CONFIRM). Then it fires a click on the delete glyph (from the hit node, as glass does on
 * pick) and checks the deletion happens and the list re-renders.
 *
 * Note: without a Scene, the StackPane handler observes source = StackPane and target = glyph
 * (see {@link IconHitTestProbeTest}), so this test exercises the payload-on-source path. The
 * real-glass discrepancy (source = glyph, payload absent there) is the reported bug and cannot be
 * reproduced headless.
 */
class BootstrapRowWiringProbeTest {

  static class InMemoryBootstrapUseCase implements BootstrapUseCase {
    final List<Bootstrap> data = new ArrayList<>(List.of(
        new Bootstrap("127.0.0.1", "5050", "QmProbePeerAAA123456789012345678901"),
        new Bootstrap("127.0.0.1", "5051", "QmProbePeerBBB123456789012345678901")));
    final List<Bootstrap> deleted = new ArrayList<>();

    @Override
    public List<Bootstrap> findAll() {
      return List.copyOf(data);
    }

    @Override
    public void save(Bootstrap bootstrap) {
      data.removeIf(b -> b.id().equals(bootstrap.id()));
      data.add(bootstrap);
    }

    @Override
    public void delete(Bootstrap bootstrap) {
      deleted.add(bootstrap);
      data.removeIf(b -> b.id().equals(bootstrap.id()));
    }

    @Override
    public boolean exists(Bootstrap bootstrap) {
      return data.stream().anyMatch(b -> b.id().equals(bootstrap.id()));
    }
  }

  static class StubAlertsCreator extends SettingsAlertsCreator {
    @Override
    public Alert createDeleteBootstrapConfirmationAlert() {
      // showAndWait() is final in JavaFX 23: stub it with mockito-inline (supports final methods).
      final Alert alert = org.mockito.Mockito.mock(Alert.class);
      org.mockito.Mockito.when(alert.showAndWait())
          .thenReturn(Optional.of(SettingsAlertsCreator.DELETE_CONFIRM));
      return alert;
    }
  }

  private static void awaitTrue(BooleanSupplier condition, String what) throws InterruptedException {
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("timeout waiting for: " + what);
      }
      TimeUnit.MILLISECONDS.sleep(20);
    }
  }

  private static SVGPath glyphOf(StackPane icon) {
    return (SVGPath) icon.getChildren().get(0);
  }

  /**
   * Fires a click from the glyph (the hit node, as glass does on pick). Without a Scene the
   * handler on the StackPane sees source = StackPane, target = glyph — see
   * {@link IconHitTestProbeTest} for the locked-in semantics.
   */
  private static void fireGlyphClick(SVGPath glyph) {
    final MouseEvent click = new MouseEvent(glyph, glyph, MouseEvent.MOUSE_CLICKED,
        8, 8, 8, 8, MouseButton.PRIMARY, 1,
        false, false, false, false, true, false, false, false, false, false, null);
    Event.fireEvent(glyph, click);
  }

  private static StackPane findDeleteIcon(ControllerNodesImpl nodes) {
    return nodes.getNodes().stream()
        .filter(n -> SETTINGS_BOOTSTRAP_DELETE_ICON.equals(n.getId()))
        .map(StackPane.class::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no delete icon registered"));
  }

  @Test
  void deleteRowThroughRealWiring() throws Exception {
    // Guard: in the full suite another test may have started the toolkit already.
    try {
      Platform.startup(() -> {
      });
    } catch (IllegalStateException alreadyInitialized) {
      // toolkit already up in this JVM — fine
    }

    final ControllerNodesImpl nodes = new ControllerNodesImpl(new ArrayList<>());
    final InMemoryBootstrapUseCase useCase = new InMemoryBootstrapUseCase();

    final AtomicReference<BootstrapListCreator> creatorRef = new AtomicReference<>();
    @SuppressWarnings("unchecked")
    final ObjectProvider<BootstrapListCreator> creatorProvider =
        org.mockito.Mockito.mock(ObjectProvider.class);
    org.mockito.Mockito.when(creatorProvider.getObject()).thenAnswer(inv -> creatorRef.get());

    final BootstrapCopyCommand copyCommand = new BootstrapCopyCommand(nodes);
    final BootstrapDeleteCommand deleteCommand =
        new BootstrapDeleteCommand(nodes, useCase, new StubAlertsCreator(), creatorProvider);
    final BootstrapListCreator creator = new BootstrapListCreator(
        nodes, useCase, copyCommand, deleteCommand, new EventsAssemblerImpl());
    creatorRef.set(creator);

    final VBox container = new VBox();
    container.setId(SETTINGS_BOOTSTRAP_LIST_CONTAINER);
    nodes.addNode(container);

    // ---- Round 1: initial render (as SettingsController.initialize does) ----
    creator.refresh();
    System.out.println("R1: rows rendered = " + container.getChildren().size());
    assertEquals(2, container.getChildren().size());

    final StackPane deleteIcon1 = findDeleteIcon(nodes);
    awaitTrue(() -> deleteIcon1.getOnMouseClicked() != null, "delete icon handler (round 1)");
    System.out.println("R1: handler attached = true");

    // Click the glyph of the FIRST delete icon (the glass layer hits the deepest node).
    final Bootstrap firstRow = useCase.findAll().get(0);
    fireGlyphClick(glyphOf(deleteIcon1));

    System.out.println("R1 after click: data=" + useCase.data.size()
        + " deleted=" + useCase.deleted.stream().map(Bootstrap::id).toList()
        + " rows=" + container.getChildren().size());
    assertEquals(List.of(firstRow), useCase.deleted, "the first row must have been deleted");
    assertEquals(1, container.getChildren().size(), "list must have re-rendered with one row");

    // ---- Round 2: re-render happened; delete the remaining row (second wiring round) ----
    final StackPane deleteIcon2 = findDeleteIcon(nodes);
    awaitTrue(() -> deleteIcon2.getOnMouseClicked() != null, "delete icon handler (round 2)");
    final Bootstrap secondRow = useCase.findAll().get(0);
    fireGlyphClick(glyphOf(deleteIcon2));

    System.out.println("R2 after click: data=" + useCase.data.size()
        + " deleted=" + useCase.deleted.stream().map(Bootstrap::id).toList()
        + " rows=" + container.getChildren().size());
    assertEquals(List.of(firstRow, secondRow), useCase.deleted, "the second row must have been deleted");
    assertEquals(0, container.getChildren().size(), "list must be empty after the last deletion");
  }
}
