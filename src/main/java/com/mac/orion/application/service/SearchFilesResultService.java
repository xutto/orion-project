package com.mac.orion.application.service;

import com.mac.orion.application.in.Publisher;
import com.mac.orion.application.in.SearchFilesResultUseCase;
import com.mac.orion.domain.dht.OperationsType;
import com.mac.orion.domain.model.File;
import com.mac.orion.domain.model.SearchResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFilesResultService implements SearchFilesResultUseCase {

  private final Publisher<SearchResult> searchFilePublisherService;
  ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  @Override
  public void sendToUISearchResults(SearchResult searchResult) {

    // an async process is necessary to avoid blocking the UI thread
    executor.execute(() -> {

      // todo EXAMPLE, PLEASE REMOVE IT
//      searchResult.getFiles().addAll(getEXMAPLEDATA());

      searchFilePublisherService.publish(OperationsType.SAVE, searchResult);
    });
  }

  private Collection<File> getEXMAPLEDATA() {
    // TEST TABLE ====================== to delete
    PodamFactory factory = new PodamFactoryImpl();
    final ArrayList<File> files = new ArrayList<>();
    for (int i = 0; i < 400; i++) {
      final File file = factory.manufacturePojo(File.class);
      files.add(file);
    }
    final ObservableList<File> filesObservable = FXCollections.observableArrayList(files);
    return filesObservable;
    // TEST TABLE ====================== to delete
  }
}
