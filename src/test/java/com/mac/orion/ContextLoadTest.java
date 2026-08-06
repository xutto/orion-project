package com.mac.orion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = {"classpath:application.yml", "classpath:application-test.yml"})
@ActiveProfiles("test")
public class ContextLoadTest {

  @Test
  void contextLoads() {
  }

}
