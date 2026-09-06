package com.mac.orion.application.out;

/**
 * Resolves the node's public IP via https://api.ipify.org.
 *
 * <p>The endpoint is the source of truth for the connection panel: the value is
 * intentionally NOT persisted (it is read-only at this point).</p>
 */
public interface PublicIpResolverUseCase {

  /**
   * @return the public IP, or null if it could not be resolved (offline, timeout, malformed body)
   */
  public String resolve();
}
