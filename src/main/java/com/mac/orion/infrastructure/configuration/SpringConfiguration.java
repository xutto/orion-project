package com.mac.orion.infrastructure.configuration;

import com.mac.orion.domain.Bootable;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SpringConfiguration {

  private final List<Bootable> bootableComponents;

  @PostConstruct
  public void runAllBootable() {
    log.info("Running all bootable components: {}", bootableComponents.toString());
    bootableComponents.forEach(Bootable::boot);
  }

}
