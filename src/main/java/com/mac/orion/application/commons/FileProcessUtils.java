package com.mac.orion.application.commons;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileProcessUtils {


  public static final String ALGORITHM_MD5 = "MD5";
  public static final int CHUNK_SIZE_BYTES = 4194304;
  private static final char[] HEX = "0123456789abcdef".toCharArray();
  public static final int INT_1024 = 1024;

  public static String checksumMD5String(Path filepath) {

    MessageDigest MD5MessageDigest;
    try {
      MD5MessageDigest = MessageDigest.getInstance(ALGORITHM_MD5);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }

    try (InputStream inputStream = Files.newInputStream(filepath);
        DigestInputStream digestInputStream = new DigestInputStream(inputStream,
            MD5MessageDigest)) {

      byte[] buf = new byte[CHUNK_SIZE_BYTES];
      // todo a config , el tamaño de bloque no afecta al resultado
      while (digestInputStream.read(buf) != -1) {
        MD5MessageDigest = digestInputStream.getMessageDigest();
      }

    } catch (IOException e) {
      log.error("Cannot process file: {}", filepath);
      throw new RuntimeException(e);
    }

    return bytesToHex(MD5MessageDigest.digest());
  }

  public static String checksumMD5FromBytes(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("data cannot be null");
    }
    try {
      MessageDigest md = MessageDigest.getInstance(ALGORITHM_MD5);
      md.update(data);
      return bytesToHex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static String checksumMD5FromByteBuffer(ByteBuffer buffer, int size) {
    final ByteBuffer view = buffer.duplicate();
    view.position(0).limit(size);
    final byte[] containerBytes = new byte[size];
    view.slice().get(containerBytes);
    return checksumMD5FromBytes(containerBytes);
  }

  public static Long convertBytesToMegaBytes(Long bytes) {
    return bytes / INT_1024 / INT_1024;
  }

/*
  private static String bytesToHex(byte[] bytes) {
    final StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
*/

  private static String bytesToHex(byte[] bytes) {
    char[] out = new char[bytes.length * 2];
    for (int i = 0, j = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      out[j++] = HEX[v >>> 4];
      out[j++] = HEX[v & 0x0F];
    }
    return new String(out);
  }
}
