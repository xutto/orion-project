package com.mac.orion.protoP2P;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom decoder for Protobuf messages that handles ByteBuf input. This class processes incoming
 * ByteBuf messages by skipping extra header bytes and continuing the processing chain.
 */
@Slf4j
public class CustomProtobufDecoder extends SimpleChannelInboundHandler<ByteBuf> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {

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
}
