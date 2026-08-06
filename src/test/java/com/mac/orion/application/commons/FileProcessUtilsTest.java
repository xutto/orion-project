package com.mac.orion.application.commons;

import com.mac.orion.BaseUnitTest;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileProcessUtilsTest extends BaseUnitTest {

  @Test
  void shouldCreate1KbRandomByteArray() {
    // Given
    int oneKb = 1024;
    byte[] randomBytes = new byte[oneKb];
    new Random().nextBytes(randomBytes);

    // Then
    Assertions.assertEquals(oneKb, randomBytes.length);
    Assertions.assertTrue(containsNonZeroBytes(randomBytes));
  }

  private boolean containsNonZeroBytes(byte[] bytes) {
    for (byte b : bytes) {
      if (b != 0) {
        return true;
      }
    }
    return false;
  }
}
