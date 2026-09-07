package com.mac.orion.application.out;

/**
 * Port for the node configuration persisted in the database (NODE_CONFIG row).
 * The database is the single source of truth for the P2P port and LimitK.
 */
public interface NodeConfigUseCase {

  /**
   * @return the P2P port (never null: the row is seeded and created with a default)
   */
  Integer getPort();

  /**
   * Persists the new P2P port.
   *
   * @param port new port (validated by the caller before persisting)
   */
  void updatePort(int port);

  /**
   * @return the LimitK cap (1..100); never null (seeded/migrated with the default)
   */
  Integer getLimitK();

  /**
   * Persists the new LimitK.
   *
   * @param limitK new value (validated by the caller, 1..100, before persisting)
   */
  void updateLimitK(int limitK);

}
