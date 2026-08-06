package com.mac.orion.infrastructure.p2p.receiver;

import com.mac.orion.application.in.SearchFilesResultUseCase;
import com.mac.orion.domain.model.SearchResult;
import com.mac.orion.infrastructure.mapper.SearchMapper;
import com.mac.orion.infrastructure.p2p.controller.FileResultsController;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchFilesResult;
import com.mac.orion.infrastructure.p2p.model.SearchResult.SearchInfoResult;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
@RequiredArgsConstructor
public class FileResultsReceiver implements ProtocolMessageHandler<SearchInfoResult>,
    FileResultsController {

  private final SearchFilesResultUseCase searchFilesResultUseCase;
  private final SearchMapper searchMapper;

  @Override
  public CompletableFuture<Boolean> send(SearchInfoResult searchInfoResult) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public CompletableFuture<PeerInfoDiscovery> sendAsync(SearchInfoResult searchInfoResult) {
    throw new IllegalStateException("Responder only!");
  }

  @Override
  public void onMessage(@NotNull Stream stream, SearchInfoResult msg) {

    final SearchFilesResult searchFilesResult = msg.getRequest();
    final SearchResult searchResult = searchMapper.mapSearchResourceFromSearchFilesResult(
        searchFilesResult);

    searchFilesResultUseCase.sendToUISearchResults(searchResult);

    stream.closeWrite();
  }


  @Override
  public void onException(@Nullable Throwable cause) {
    log.error("Exception received ", cause);
  }

  @Override
  public void onClosed(@NotNull Stream stream) {
    log.info("Stream receiver FileResults closed: {}", stream);
  }

}
