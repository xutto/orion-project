package com.mac.orion.infrastructure.ui.controller;

import com.mac.orion.domain.model.Peer;
import com.mac.orion.infrastructure.ui.nodes.ControllerNodes;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ConnectionController implements UIController{

    private final ControllerNodes connectionControllerNodes;

    @FXML
    private TableView<Peer> connTable;
    @FXML
    private TableColumn<Peer, String> connIdTableColumn;
    @FXML
    private TableColumn<Peer, String> connHashColumn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        connTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // todo arreglar esto , los datos que se exponen aqui no son correctos
        connIdTableColumn.setCellValueFactory(k -> new SimpleStringProperty(k.getValue().getId()));
        connHashColumn.setCellValueFactory(
            k -> new SimpleStringProperty(k.getValue().getHash()));


//        // TEST TABLE ====================== to delete
//        PodamFactory factory = new PodamFactoryImpl();
//        final ArrayList<HostNode> files = new ArrayList<>();
//        for (int i = 0; i < 40; i++) {
//            final HostNode hostNode = factory.manufacturePojo(HostNode.class);
//            files.add(hostNode);
//        }
//        final ObservableList<HostNode> filesObservable = FXCollections.observableArrayList(files);
//        connTable.setItems(filesObservable);
//        // TEST TABLE ====================== to delete


        connectionControllerNodes.addNode(connTable);

    }
}
