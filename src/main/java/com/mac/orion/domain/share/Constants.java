package com.mac.orion.domain.share;

public class Constants {

  public static final String PROTOCOL_P2P = "/p2p/";
  public static String ADDRESS_SEPARATOR_REGEX = "/";

  /**
   * Default P2P port used to seed NODE_CONFIG (database is the single source of truth at runtime).
   */
  public static final int P2P_PORT_DEFAULT = 4050;

  /**
   * Default LimitK (closest-peers cap in discovery replies); valid range is 1..100.
   */
  public static final int LIMIT_K_DEFAULT = 20;

}
