package com.mac.orion.application.service;

import com.mac.orion.application.in.FileAllocatorUseCase;
import com.mac.orion.application.in.TransferControlUseCase;
import com.mac.orion.application.out.FilesPort;
import com.mac.orion.application.service.agent.GlobalDownloadAgent;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.Hash;
import com.mac.orion.domain.model.Status;
import com.mac.orion.domain.model.Transfer;
import com.mac.orion.domain.model.transfer.Sidecar;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransferControlService implements TransferControlUseCase {

  private final FilesPort filesPort;
  private final FileAllocatorUseCase fileAllocatorService;
  private final GlobalDownloadAgent globalDownloadAgent;


  @Override
  public Transfer getDataTransfer(Hash hash) {
    log.info("Getting transfer data for hash {}", hash);
    final File file = filesPort.findByHash(hash.getValue());

    if (file.getStatus() != Status.DOWNLOADING){ // TODO must check if exists but is not downloading, means is a finalized file
      log.warn("File not downloading");
      return null;
    }

    final Set<Integer> ongoingDownloads = globalDownloadAgent.getOngoingFragmentsDownloads(hash);
    Transfer.TransferBuilder transfer = Transfer.builder();
    transfer.hash(file.getHash())
        .size(file.getSize())
        .names(file.getNames())
        .transfersOnFly(ongoingDownloads);

    try {
      final Sidecar sidecar = fileAllocatorService.loadSidecarBitset(hash);
      transfer.totalFragments(sidecar.getTotalFragments());
      transfer.transfersCompleted(sidecar.getData().stream().boxed().toList());

    } catch (IOException e) {
      log.error("Error loading sidecar bitset", e);
      return null;
    }


    return transfer.build();
  }

  /*

  import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import java.util.List;

public class ChunkProgressBar extends Region {
    private final Canvas canvas = new Canvas();

    public ChunkProgressBar() {
        getChildren().add(canvas);
        // Esto une el tamaño del dibujo al tamaño de la celda de la tabla
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // Cada vez que la tabla se redimensiona, redibujamos
        canvas.widthProperty().addListener(e -> draw(null, 0));
    }

    public void update(List<Integer> completedParts, int totalParts) {
        draw(completedParts, totalParts);
    }

    private void draw(List<Integer> parts, int total) {
        if (total <= 0) return;

        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.clearRect(0, 0, w, h); // Limpiar fondo
        double step = w / total;  // Calcular ancho de cada fragmento

        // Dibujamos el fondo (gris)
        gc.setFill(Color.web("#3e3e3e"));
        gc.fillRect(0, 0, w, h);

        // Dibujamos los fragmentos completados (verde)
        if (parts != null) {
            gc.setFill(Color.LIMEGREEN);
            for (Integer partIndex : parts) {
                // Posición X = índice de la parte * ancho de cada fragmento
                gc.fillRect(partIndex * step, 0, step, h);
            }
        }
    }
}

// 1. Cambiamos el tipo de la columna de Double a List<Integer> (o el objeto Transfer entero)
@FXML
private TableColumn<Transfer, Transfer> transfersProgressColumn;

// 2. En el initialize:
transfersProgressColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

transfersProgressColumn.setCellFactory(col -> new TableCell<>() {
    // Creamos una instancia del componente para esta celda específica
    private final ChunkProgressBar chunkBar = new ChunkProgressBar();

    @Override
    protected void updateItem(Transfer transfer, boolean empty) {
        super.updateItem(transfer, empty);
        if (empty || transfer == null) {
            setGraphic(null);
        } else {
            // Pasamos los datos que ya tienes en el modelo Transfer
            chunkBar.update(transfer.getTransferStatus(), transfer.getTotalFragments());
            setGraphic(chunkBar); // Metemos el canvas dentro de la celda
        }
    }
});

   */
}

