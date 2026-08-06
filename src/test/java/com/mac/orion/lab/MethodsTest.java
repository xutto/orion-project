package com.mac.orion.lab;

import java.text.Normalizer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MethodsTest {

  @Test
  void triGramTest() {
    final String name = "name of my favorite film ever";

    final String normalizedName = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replaceAll("[^\\p{ASCII}]", "")
        .toLowerCase();

    final Set<String> trigrams = IntStream.range(0, normalizedName.length() - 2)
        .mapToObj(i -> name.substring(i, i + 3)).collect(Collectors.toSet());


    trigrams.forEach(t -> log.info("Trigram: {}", t));
  }
}
