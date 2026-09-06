package com.mac.orion.infrastructure.ui.connection;

import com.mac.orion.domain.model.settings.Settings;
import io.libp2p.core.Host;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Service
@RequiredArgsConstructor
public class ConnectionInfoProvider {

  public static final String IP_DEFAULT_LOCALHOST = "127.0.0.1";
  private final Host hostNode;
  private final Settings settings;

  /**
   * Primera IPv4 no-loopback de una interfaz activa.
   * Fallback: 127.0.0.1
   *
   * @return the local ip address as string
   */
  public String getLocalIp() {
    try {
      final Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
      while (ifaces != null && ifaces.hasMoreElements()) {
        final NetworkInterface iface = ifaces.nextElement();
        if (!iface.isUp() || iface.isLoopback()) {
          continue;
        }
        final Enumeration<InetAddress> addrs = iface.getInetAddresses();
        while (addrs.hasMoreElements()) {
          final InetAddress addr = addrs.nextElement();
          if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
            return addr.getHostAddress();
          }
        }
      }
    } catch (SocketException e) {
      // fallback to loopback
    }
    return IP_DEFAULT_LOCALHOST;
  }

  /**
   * @return the p2p client port from settings (String)
   */
  public String getPort() {
    return settings.getP2p().client().port();
  }

  /**
   * @return the own host peer id (base58)
   */
  public String getPeerId() {
    return hostNode.getPeerId().toString();
  }
}
