package com.mac.orion.infrastructure.ui.creation;

import com.mac.orion.domain.model.Transfer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class FragmentProgressBar extends Region implements Creator{

  private Canvas canvas;
//  private List<Integer> lastParts;
//  private int lastTotal;
  private Transfer transfer;

  @Override
  public void init() {
    this.canvas = new Canvas();
  }

  public FragmentProgressBar create() {

    getChildren().add(canvas);
    // Bind para que el canvas siga al contenedor
    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());

    // Redibujar cuando cambie el tamaño, usando los últimos datos conocidos
    widthProperty().addListener((obs, oldVal, newVal) -> draw(transfer));
    heightProperty().addListener((obs, oldVal, newVal) -> draw(transfer));

    return this;
  }

//
//  public FragmentProgressBar() {
//    getChildren().add(canvas);
//    // Bind para que el canvas siga al contenedor
//    canvas.widthProperty().bind(widthProperty());
//    canvas.heightProperty().bind(heightProperty());
//
//    // Redibujar cuando cambie el tamaño, usando los últimos datos conocidos
//    widthProperty().addListener((obs, oldVal, newVal) -> draw(lastParts, lastTotal));
//    heightProperty().addListener((obs, oldVal, newVal) -> draw(lastParts, lastTotal));
//  }

  public void update(Transfer transfer) {
    draw(transfer);
  }

  private void draw(Transfer transfer) {
    if (transfer.getTotalFragments() <= 0) return;

    double w = getWidth();
    double h = getHeight();
    GraphicsContext gc = canvas.getGraphicsContext2D();

    gc.clearRect(0, 0, w, h); // Limpiar fondo
    double step = w / transfer.getTotalFragments();  // Calcular ancho de cada fragmento

    // Dibujamos el fondo (gris)
    gc.setFill(Color.web("#0a0a0a")); // todo config [progressbar background color]
    gc.fillRect(0, 0, w, h);

    if (transfer.getTransfersOnFly() != null) {
      gc.setFill(Color.YELLOW); // todo config [progressbar on air transfer color]
      for (Integer partIndex : transfer.getTransfersOnFly()) {
        // Posición X = índice de la parte * ancho de cada fragmento
        gc.fillRect(partIndex * step, 0, step, h);
      }
    }
    // Dibujamos los fragmentos completados (verde)
    if (transfer.getTransfersCompleted() != null) {
      gc.setFill(Color.LIMEGREEN); // todo config [progressbar completed color]
      for (Integer partIndex : transfer.getTransfersCompleted()) {
        // Posición X = índice de la parte * ancho de cada fragmento
        gc.fillRect(partIndex * step, 0, step, h);
      }
    }
  }



}
