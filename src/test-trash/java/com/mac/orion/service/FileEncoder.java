package com.mac.orion.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FileEncoder extends MessageToByteEncoder<byte[]> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, byte[] msg, ByteBuf out) throws Exception {
        // Escribe el tamaño del archivo primero (si es necesario)
        out.writeInt(msg.length);
        // Escribe los bytes del archivo
        out.writeBytes(msg);
    }
}
