package com.mac.orion.infrastructure.mapper;

import com.google.protobuf.ByteString;
import java.time.Instant;
import java.util.BitSet;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BytesMapper {

  @Named("byteSTRToString")
  default String byteToString(ByteString bytes) {
    if (bytes == null || bytes.isEmpty()) {
      return null;
    }
    return bytes.toStringUtf8();
  }

  @Named("stringToByteSTR")
  default ByteString stringToByte(String string) {
    if (string == null || string.isEmpty()) {
      return null;
    }
    return ByteString.copyFromUtf8(string);
  }
  
  @Named("byteSTRToBytes")
  default byte[] byteSTRToBytes(ByteString byteString) {
    if (byteString == null || byteString.isEmpty()) {
      return null;
    }
    return byteString.toByteArray();
  }

  @Named("bytesToByteSTR")
  default ByteString bytesToByteSTR(byte[] data) {
    if (data == null) {
      return null;
    }
    return ByteString.copyFrom(data);
  }
  
  
  @Named("byteSTRToBitSet")
  default BitSet byteStringToBitSet(ByteString byteString) {
    if (byteString == null || byteString.isEmpty()) {
      return null; // or return null;
    }
    return BitSet.valueOf(byteString.toByteArray());
  }

  @Named("bitSetToByteSTR")
  default ByteString bitSetToByteString(BitSet bitSet) {
    if (bitSet == null) {
      return null; // o null; según tu modelo
    }
    return ByteString.copyFrom(bitSet.toByteArray());
  }
  

  @Named("longToInstant")
  default Instant longToInstant(Long longValue) {
    return Instant.ofEpochMilli(longValue);
  }

  @Named("instantToLong")
  default Long instantToLong(Instant instant) {
    return instant.toEpochMilli();
  }

}
