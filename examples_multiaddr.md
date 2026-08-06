# Ejemplos habituales de `Multiaddr.toString()`

| Escenario                         | Ejemplo de `Multiaddr.toString()`                    | Comentario breve                                            |
|-----------------------------------|------------------------------------------------------|-------------------------------------------------------------|
| **Loopback IPv4**                 | `/ip4/127.0.0.1/tcp/4001`                            | Pruebas en la misma máquina                                 |
| **LAN doméstica (192.168.x)**     | `/ip4/192.168.0.42/tcp/4001`                         | Dirección privada típica de router doméstico                |
| **LAN corporativa (10.x)**        | `/ip4/10.12.5.7/tcp/4001`                            | Otro rango site‑local                                       |
| **Loopback IPv6**                 | `/ip6/::1/tcp/4001`                                  | Equivalente IPv6 de 127.0.0.1                               |
| **IPv6 global**                   | `/ip6/2001:db8:abcd:1::5/tcp/4001`                   | Ejemplo con bloque de documentación                         |
| **IP pública (port‑forward)**     | `/ip4/203.0.113.55/tcp/4001`                         | Peer accesible desde Internet                               |
| **Con Peer ID incluido**          | `/ip4/203.0.113.55/tcp/4001/p2p/12D3KooW…`           | Suele añadirse al anunciar la dirección completa            |
| **DNS4**                          | `/dns4/seed.libp2p.io/tcp/4001`                      | Resolución por nombre (IPv4)                                |
| **QUIC/UDP**                      | `/ip4/203.0.113.55/udp/4001/quic`                    | Transporte QUIC sobre UDP                                   |
| **WebSocket**                     | `/ip4/192.168.0.42/tcp/8080/ws`                      | Nodo escuchando en WebSocket plano                          |
| **WebTransport**                  | `/ip4/203.0.113.55/udp/443/webtransport`             | Usado desde navegador                                       |
| **Relé p2p‑circuit**              | `/p2p/12D3Krelay/p2p-circuit/p2p/12D3Kdest`          | Ruta mediante relé cuando el destino está tras NAT estricto |
| **Dirección observada (AutoNAT)** | `/ip4/203.0.113.55/tcp/4001/p2p/12D3Kxx/observation` | Añadido por algunos dials AutoNAT                           |

> Todos los segmentos siguen la sintaxis *protocolo/valor*: `ip4/`, `tcp/`, `udp/`, etc. Los sufijos
> como `p2p`, `ws`, `quic` se añaden al final para indicar capa o identificador extra.

