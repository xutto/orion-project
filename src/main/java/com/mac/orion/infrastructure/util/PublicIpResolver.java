package com.mac.orion.infrastructure.util;

import com.mac.orion.application.out.PublicIpResolverUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Resolves the node's public IP via https://api.ipify.org.
 *
 * <p>The endpoint is the source of truth for the connection panel: the value is
 * intentionally NOT persisted (it is read-only at this point).</p>
 */
@Component
@Slf4j
public class PublicIpResolver implements PublicIpResolverUseCase {

  private static final String IP_PUBLIC_ENDPOINT = "https://api.ipify.org";
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final HttpClient client = HttpClient.newBuilder()
      .connectTimeout(TIMEOUT)
      .build();

  /**
   * @return the public IP, or null if it could not be resolved (offline, timeout, malformed body)
   */
  public String resolve() {
    try {
      final HttpResponse<String> response = client.send(
          HttpRequest.newBuilder(URI.create(IP_PUBLIC_ENDPOINT))
              .GET()
              .timeout(TIMEOUT)
              .build(),
          HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Public IP endpoint returned status {}", response.statusCode());
        return null;
      }
      final String ip = response.body() == null ? null : response.body().trim();
      if (ip == null || ip.isBlank()) {
        log.warn("Public IP endpoint returned an empty body");
        return null;
      }
      log.info("Public IP resolved: {}", ip);
      return ip;
    } catch (Exception e) {
      log.warn("Failed to resolve the public IP from {}", IP_PUBLIC_ENDPOINT, e);
      return null;
    }
  }
}
