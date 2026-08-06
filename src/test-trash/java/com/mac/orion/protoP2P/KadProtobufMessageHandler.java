package com.mac.orion.protoP2P;

import com.mac.orion.infrastructure.p2p.model.PeerAnnounce.PeerInfoRequest;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtobufProtocolHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KadProtobufMessageHandler extends ProtobufProtocolHandler<KadController> {

  public static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 10mb
  public static final String LIMITED_PROTOBUF_VARINT_32_FRAME_DECODER_0 = "LimitedProtobufVarint32FrameDecoder#0";
  public static final String CUSTOM_PROTOBUF_DECODER = "customProtobufDecoder";

  public KadProtobufMessageHandler() {
    super(PeerInfoRequest.getDefaultInstance(), MAX_MESSAGE_SIZE, MAX_MESSAGE_SIZE);
  }

  @NotNull
  @Override
  protected CompletableFuture<KadController> onStartInitiator(@NotNull Stream stream) {
    log.info("onStartInitiator invoked for stream: {}", stream);
    final KadSenderHandler kadSenderHandler = new KadSenderHandler(stream);
    stream.pushHandler(kadSenderHandler);
    return CompletableFuture.completedFuture(kadSenderHandler);
  }

  @NotNull
  @Override
  protected CompletableFuture<KadController> onStartResponder(@NotNull Stream stream) {

    // added custom decoder by fixed header issue
    // todo delete hack when libp2p is fixed
    stream.pushHandler(new ChannelInitializer<>() {
      @Override
      protected void initChannel(Channel ch) {
        ch.pipeline().addBefore(LIMITED_PROTOBUF_VARINT_32_FRAME_DECODER_0, CUSTOM_PROTOBUF_DECODER,
            new CustomProtobufDecoder());
      }
    });

    log.info("onStartResponder invoked for stream: {}", stream);
    KadReceiverHandler kadReceiverHandler = new KadReceiverHandler();
    stream.pushHandler(kadReceiverHandler);

    return CompletableFuture.completedFuture(kadReceiverHandler);
  }


}
