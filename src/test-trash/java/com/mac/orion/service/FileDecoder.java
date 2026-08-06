package com.mac.orion.service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

public class FileDecoder extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {

        // Verifica si tenemos suficientes bytes para leer (puedes ajustar la lógica según lo que esperas recibir)
        if (in.readableBytes() < 4) {
            return; // Si no hay suficientes datos, espera más bytes
        }

        // Lee el tamaño del archivo
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex(); // Vuelve al índice anterior si no hay suficientes bytes
            return;
        }

        // Lee los bytes del archivo
        byte[] fileBytes = new byte[length];
        in.readBytes(fileBytes);

        // Añade los bytes decodificados a la lista de salida
        out.add(fileBytes);
    }
}

