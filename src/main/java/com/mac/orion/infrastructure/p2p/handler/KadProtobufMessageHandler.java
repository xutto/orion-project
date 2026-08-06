package com.mac.orion.infrastructure.p2p.handler;

import com.mac.orion.application.in.KadDiscoveryUseCase;
import com.mac.orion.infrastructure.mapper.PeerMapper;
import com.mac.orion.infrastructure.p2p.controller.KadController;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import com.mac.orion.infrastructure.p2p.receiver.KadReceiver;
import com.mac.orion.infrastructure.p2p.sender.KadSender;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KadProtobufMessageHandler extends ProtobufProtocolHandler<KadController> {

  public static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 10mb
  private final KadDiscoveryUseCase kadDiscoveryProcessService;
  private final PeerMapper peerMapper;

  public KadProtobufMessageHandler(KadDiscoveryUseCase kadDiscoveryProcessService,
      PeerMapper peerMapper) {
    super(PeerInfoDiscovery.getDefaultInstance(), MAX_MESSAGE_SIZE, MAX_MESSAGE_SIZE);
    this.peerMapper = peerMapper;
    this.kadDiscoveryProcessService = kadDiscoveryProcessService;
  }

  @NotNull
  @Override
  protected CompletableFuture<KadController> onStartInitiator(@NotNull Stream stream) {
    log.info("onStartInitiator invoked for stream: {}", stream);
    final KadSender kadSender = new KadSender(stream,
        kadDiscoveryProcessService, peerMapper);
    stream.pushHandler(kadSender);
//    return CompletableFuture.completedFuture(kadSender);

    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(100); // 50ms debería ser suficiente
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return kadSender;
    });
  }

  @NotNull
  @Override
  protected CompletableFuture<KadController> onStartResponder(@NotNull Stream stream) {

    log.info("onStartResponder invoked for stream: {}", stream);
    KadReceiver kadReceiver = new KadReceiver(kadDiscoveryProcessService, peerMapper);
    stream.pushHandler(kadReceiver);

    return CompletableFuture.completedFuture(kadReceiver);
  }

//
//  @Override
//  protected void initProtocolStream(@NotNull Stream stream) {
////    stream.pushHandler(new LimitedProtobufVarint32FrameDecoder(MAX_MESSAGE_SIZE / 3));
//    stream.pushHandler(new LimitedProtobufVarint32FrameDecoder(MAX_MESSAGE_SIZE / 3));
//    stream.pushHandler(new ProtobufVarint32LengthFieldPrepender());
//    stream.pushHandler(new ProtobufDecoder(PeerInfoDiscovery.getDefaultInstance()));
//    stream.pushHandler(new ProtobufEncoder());
//  }


}
