package com.mac.orion.infrastructure.p2p.decoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class CustomProtobufDecoderTest {

    private final EmbeddedChannel embeddedChannel = new EmbeddedChannel();

    @AfterEach
    void tearDown() {
        embeddedChannel.finishAndReleaseAll();
    }

    //@Test
    void shouldProcessDataPayloadWithVarIntHeader() {

        // Configura el ByteBuf con encabezado Varint (1 bytes) + datos
        ByteBuf buffer = Unpooled.buffer();
        byte[] dataPayload = "Sefirot is here, run...".getBytes(StandardCharsets.UTF_8);
        buffer.writeBytes(encodeVarInt(dataPayload.length));
        buffer.writeBytes(dataPayload);

        // Simula el flujo de entrada al canal
        embeddedChannel.writeInbound(buffer);

        // Lee el resultado del flujo procesado
        ByteBuf outputBuffer = embeddedChannel.readInbound();
        byte[] resultingData = new byte[outputBuffer.readableBytes()];
        outputBuffer.readBytes(resultingData);

        // Asegura que el encabezado fue eliminado y los datos son procesados correctamente
        assertArrayEquals(dataPayload, resultingData);
        log.info("resultingData: {}", new String(resultingData, StandardCharsets.UTF_8));

        // Libera el buffer
        ReferenceCountUtil.release(outputBuffer);
    }

    //@Test
    void shouldRemoveHeaderBytesAndPreservePayload() {

        // Configura el ByteBuf con encabezado Varint (1 bytes) + datos
        ByteBuf buffer = Unpooled.buffer();
        byte[] dataPayload = new byte[]{10, 20, 30, 40}; // Datos reales
        buffer.writeBytes(new byte[]{(byte) 127}); // Simula encabezado (1 bytes)
        buffer.writeBytes(dataPayload);

        // Simula el flujo de entrada al canal
        embeddedChannel.writeInbound(buffer);

        // Lee el resultado del flujo procesado
        ByteBuf outputBuffer = embeddedChannel.readInbound();
        byte[] resultingData = new byte[outputBuffer.readableBytes()];
        outputBuffer.readBytes(resultingData);

        // Asegura que el encabezado fue eliminado y los datos son procesados correctamente
        assertArrayEquals(dataPayload, resultingData);

        // Libera el buffer
        ReferenceCountUtil.release(outputBuffer);

    }
    
    private byte[] encodeVarInt(int value) {
        List<Byte> byteList = new ArrayList<>();

        do {
            int currentByte = value & 0b0111_1111; // toma los últimos 7 bits
            value >>>= 7;                          // desplaza 7 bits a la derecha
            if (value != 0) {
                currentByte |= 0b1000_0000; // activa el bit de continuación
            }
            byteList.add((byte) currentByte);
        } while (value != 0);

        // convertir la lista a array
        byte[] result = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            result[i] = byteList.get(i);
        }
        return result;
    }
}
