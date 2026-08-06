package com.mac.orion.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class Address {

  private String ip;
  private Integer port;
  private String type;
  private String transmission;

  /**
   * Builds and returns a formatted address chain string. The address chain is constructed using the
   * properties of the Address object, specifically type, IP, transmission protocol, and port.
   * Example: /ip4/127.0.0.1/tcp/4001
   *
   * @return a string representation of the address chain in the format
   * "/{type}/{ip}/{transmission}/{port}"
   */
  public String buildAddressChain() {
    return String.format("/%s/%s/%s/%s", getType(), getIp(), getTransmission(), getPort());
  }

}
