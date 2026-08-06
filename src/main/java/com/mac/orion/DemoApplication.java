package com.mac.orion;

import io.github.seujorgenochurras.DefaultAsciifier;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DemoApplication {

  public static void main(String[] args) {

    final Path path = Path.of("src/main/resources/icon/logo_64.png");

    String imageAsciiArt= DefaultAsciifier.toAscii(path.toAbsolutePath().toString(),64,32,false);

    log.info("booting  demo... \n\n\n\n");
    log.info("\n\n\n\n{}", imageAsciiArt);
    DemoApplicationLoader.boot(args);
  }

}
