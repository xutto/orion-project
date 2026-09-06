package com.mac.orion.application.out;

public interface ConnectionInfoProviderUseCase {
  String getLocalIp();

  String getPort();

  String getPeerId();

  void refreshPublicIp();

  void updateIpLabel();
}
