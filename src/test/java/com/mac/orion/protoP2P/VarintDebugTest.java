package com.mac.orion.protoP2P;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.mac.orion.BaseManualTest;
import com.mac.orion.infrastructure.p2p.model.Discovery.DiscoveryRequest;
import com.mac.orion.infrastructure.p2p.model.Discovery.PeerInfoDiscovery;
import io.libp2p.core.Host;
import io.libp2p.core.Stream;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.dsl.HostBuilder;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.core.multistream.ProtocolDescriptor;
import io.libp2p.core.multistream.StrictProtocolBinding;
import io.libp2p.core.mux.StreamMuxerProtocol;
import io.libp2p.protocol.ProtobufProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.libp2p.security.noise.NoiseXXSecureChannel;
import io.libp2p.transport.tcp.TcpTransport;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

@Slf4j
public class VarintDebugTest  extends BaseManualTest {

  @Test
  public void testSimpleProtobufMessage() throws Exception {
    // Crea tu mensaje protobuf más simple
    // Por ejemplo: PeerInfoDiscovery con solo un campo
    MessageLite testMessage = PeerInfoDiscovery.newBuilder()
        .setRequest(DiscoveryRequest.newBuilder()
            .setId(ByteString.copyFromUtf8("test-id"))
            .build())
        .build();

    // Servidor que recibe mensajes
    Host serverHost = new HostBuilder()
        .transport(TcpTransport::new)
        .secureChannel(NoiseXXSecureChannel::new)
        .muxer(StreamMuxerProtocol::getYamux)
        .protocol(new TestProtocol())
        .listen("/ip4/127.0.0.1/tcp/40001")
        .build();

    // Cliente que envía mensajes
    Host clientHost = new HostBuilder()
        .transport(TcpTransport::new)
        .secureChannel(NoiseXXSecureChannel::new)
        .muxer(StreamMuxerProtocol::getYamux)
        .build();

    serverHost.start().get(5, TimeUnit.SECONDS);
    clientHost.start().get(5, TimeUnit.SECONDS);

    // Conectar y enviar mensaje

    clientHost.getNetwork()
        .connect(serverHost.getPeerId(), new Multiaddr("/ip4/127.0.0.1/tcp/40001"))
        .thenApply(conn -> conn.muxerSession().createStream(new TestProtocol()))
        .thenCompose(StreamPromise::getController)
        .thenApply(controller -> {
          // Envía el mensaje - esto debería fallar con el bug del doble varint
          controller.send(testMessage);
          return controller;
        })
        .get(5, TimeUnit.SECONDS);

    // Espera para ver si el servidor recibe correctamente
    Thread.sleep(1000);

    clientHost.stop().get(5, TimeUnit.SECONDS);
    serverHost.stop().get(5, TimeUnit.SECONDS);
  }

  // Protocolo de prueba mínimo
  public class TestProtocol extends StrictProtocolBinding<TestController> {

    public TestProtocol() {
      super("TestProtocol", new TestProtobufHandler());
    }

    @Override
    public ProtocolDescriptor getProtocolDescriptor() {
      return new ProtocolDescriptor(List.of("/test/1.0.0"));
    }
  }

  interface TestController {

    void send(MessageLite msg);
  }


  class TestSender implements ProtocolMessageHandler<MessageLite>, TestController {

    private final Stream stream;

    TestSender(Stream stream) {
      this.stream = stream;
    }

    @Override
    public void send(MessageLite msg) {
      System.out.println("SENDER: Enviando mensaje de " + msg.getSerializedSize() + " bytes");
      stream.writeAndFlush(msg);
    }

    @Override
    public void onMessage(Stream stream, MessageLite msg) {
    }
  }

  class TestReceiver implements ProtocolMessageHandler<MessageLite>, TestController {

    @Override
    public void send(MessageLite msg) {
      throw new UnsupportedOperationException("Receiver no puede enviar");
    }

    @Override
    public void onMessage(Stream stream, MessageLite msg) {
      System.out.println("RECEIVER: Recibido mensaje de " + msg.getSerializedSize() + " bytes");
      System.out.println("RECEIVER: Contenido: " + msg);
    }

    @Override
    public void onException(Throwable cause) {
      System.err.println("RECEIVER ERROR: " + cause.getMessage());
      cause.printStackTrace();
    }
  }

  class TestProtobufHandler extends ProtobufProtocolHandler<TestController> {

    public TestProtobufHandler() {
      super(
          DiscoveryRequest.getDefaultInstance(),  // Tu mensaje protobuf
          1024 * 1024,  // Max size initiator
          1024 * 1024   // Max size responder
      );
    }

    @Override
    protected void initProtocolStream(@NotNull Stream stream) {

      stream.pushHandler(new ChannelInboundHandlerAdapter() {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
          log.info("Pipeline handlers: {}", ctx.pipeline().names());
          super.channelActive(ctx);
        }
      });

//      stream.pushHandler(new LimitedProtobufVarint32FrameDecoder(1024 * 1024));
      stream.pushHandler(new ProtobufVarint32FrameDecoder());
      stream.pushHandler(new ProtobufVarint32FrameDecoder());
      stream.pushHandler(new ProtobufVarint32LengthFieldPrepender());
      stream.pushHandler(new ProtobufDecoder(DiscoveryRequest.getDefaultInstance()));
      stream.pushHandler(new ProtobufEncoder());
    }


    @Override
    protected CompletableFuture<TestController> onStartInitiator(Stream stream) {
      TestSender sender = new TestSender(stream);
      stream.pushHandler(sender);
      return CompletableFuture.completedFuture(sender);
    }

    @Override
    protected CompletableFuture<TestController> onStartResponder(Stream stream) {
      TestReceiver receiver = new TestReceiver();
      stream.pushHandler(receiver);
      return CompletableFuture.completedFuture(receiver);
    }
  }
}
