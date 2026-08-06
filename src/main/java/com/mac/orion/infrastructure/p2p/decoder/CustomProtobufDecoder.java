package com.mac.orion.infrastructure.p2p.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom decoder for Protobuf messages that handles ByteBuf input. This class processes incoming
 * ByteBuf messages by skipping extra header bytes and continuing the processing chain.
 */
@Slf4j
@Deprecated
public class CustomProtobufDecoder /*extends SimpleChannelInboundHandler<ByteBuf>*/ /*extends
    ByteToMessageDecoder*/ {

  //@Override // original
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
    log.info("CustomProtobufDecoder received {} bytes", msg.readableBytes());
    log.info("First 10 bytes: {}", ByteBufUtil.hexDump(msg, 0, Math.min(10, msg.readableBytes())));
    log.debug("Data after LimitedProtobufVarint32FrameDecoder: {}", (msg.readableBytes()));

    msg.skipBytes(determineExtraBytes(msg));
    byte[] data = new byte[msg.readableBytes()];
    msg.getBytes(msg.readerIndex(), data);

    log.debug("Data after CustomProtobufDecoder: {}", (msg.readableBytes()));
    ctx.fireChannelRead(msg.retain()); // Continue the flow
  }

  private int determineExtraBytes(ByteBuf msg) {
    int readableBytes = msg.readableBytes(); // Total size of the read buffer (header + data)
    int headerBytes = 0;

    do {
      headerBytes++;
      readableBytes >>>= 7; // Shift 7 bits to the right for each header byte | 7 bits (2^7 = 128)
    } while (readableBytes > 0); // While there is more information to encode, increase the counter

    return headerBytes; // Returns the number of header bytes
  }


  /*
  private static final AttributeKey<Boolean> STRIPPED = AttributeKey.valueOf("strip.leading.varint.done");
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

    log.info("CustomProtobufDecoder received {} bytes", in.readableBytes());
    log.info("First 10 bytes: {}", ByteBufUtil.hexDump(in, 0, Math.min(10, in.readableBytes())));

    Boolean alreadyStripped = ctx.channel().attr(STRIPPED).get();

    // Si ya strippeamos en este stream, no tocar más; pasar tal cual (copia segura)
    if (Boolean.TRUE.equals(alreadyStripped)) {
      if (in.isReadable()) {
        ByteBuf copy = Unpooled.copiedBuffer(in); // copia independiente
        in.skipBytes(in.readableBytes());         // consumir del acumulador
        out.add(copy);
      }
      return;
    }

    // Intentar leer varint32 completo (máx 5 bytes)
    in.markReaderIndex();
    int varintBytes = 0;
    while (true) {
      if (!in.isReadable()) { // fragmentado: esperar más
        in.resetReaderIndex();
        return;
      }
      byte b = in.readByte();
      varintBytes++;
      if ((b & 0x80) == 0) break; // fin varint
      if (varintBytes == 5) { // inválido o muy largo
        in.resetReaderIndex();
        return; // o lanza excepción si lo prefieres
      }
    }

    // Marcamos que ya strippeamos para este stream
    ctx.channel().attr(STRIPPED).set(Boolean.TRUE);

    // Consumir lo restante y emitir copia segura
    if (in.isReadable()) {
      ByteBuf copy = Unpooled.copiedBuffer(in);
      in.skipBytes(in.readableBytes());
      out.add(copy);
    }
  }

 */
/*
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    in.markReaderIndex();

    int varintBytes = 0;
    // Intentar leer un varint32 (máximo 5 bytes). No dependas de readableBytes total.
    while (true) {
      if (!in.isReadable()) {
        in.resetReaderIndex();
        return; // esperar más datos
      }
      byte b = in.readByte();
      varintBytes++;
      if ((b & 0x80) == 0) {
        break; // fin del varint (MSB=0)
      }
      if (varintBytes == 5) {
        // varint demasiado largo o corrupto
        in.resetReaderIndex();
        return; // o lanzar excepción según tu política
      }
    }

    // Hemos avanzado exactamente 'varintBytes'. No emitimos nada: dejamos que el siguiente
    // handler (p. ej. ProtobufVarint32FrameDecoder) corte el frame de forma estándar.
    // Simplemente devolvemos para que Netty vuelva a invocar decode cuando haya más datos
    // o para que el siguiente handler procese el buffer ajustado.
    out.add(in.readRetainedSlice(in.readableBytes()));
  }
  */
}
