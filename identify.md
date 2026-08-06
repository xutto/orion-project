# Guía: Obtener Direcciones Reales de Peers en libp2p

## Problema

Cuando usas `stream.connection.remoteAddress()` en tu protocolo personalizado, obtienes la dirección
del puerto efímero del cliente (ej: `127.0.0.1:58820`) en lugar de la dirección de escucha real del
peer remoto (ej: `127.0.0.1:4001`).

## Causa del Problema

El método `remoteAddress()` devuelve la dirección del socket TCP desde donde se originó la
conexión [1](#4-0) . En TCP:

- **Cliente**: Usa un puerto efímero aleatorio para conexiones salientes
- **Servidor**: Escucha en un puerto fijo conocido

## Solución: Protocolo Identify

Para obtener las direcciones de escucha reales, debes usar el protocolo **Identify** de
libp2p [2](#4-1) .

### Flujo de Implementación

1. **Primera llamada**: Ejecutar protocolo `Identify` para obtener direcciones reales
2. **Segunda llamada**: Usar tu protocolo personalizado con las direcciones correctas

### Código de Ejemplo (Java)

```java
// 1. Primera llamada: Identify
Identify identify = new Identify();
StreamPromise<IdentifyController> identifyPromise = identify.dial(
    clientHost, 
    remotePeerId, 
    temporaryAddress // dirección inicial que tienes
);

IdentifyController controller = identifyPromise.getController().get(5, TimeUnit.SECONDS);
IdentifyOuterClass.Identify remoteInfo = controller.id().get(5, TimeUnit.SECONDS);

// Extraer las direcciones de escucha reales
List<Multiaddr> realListenAddresses = remoteInfo.getListenAddrsList().stream()
    .map(addr -> Multiaddr.deserialize(addr.toByteArray()))
    .collect(Collectors.toList());

// 2. Segunda llamada: Tu protocolo personalizado
MiProtocoloPersonalizado miProtocolo = new MiProtocoloPersonalizado();
StreamPromise<MiController> miPromise = miProtocolo.dial(
    clientHost,
    remotePeerId,
    realListenAddresses.get(0) // usar dirección real de escucha
);
```

## Información Técnica

### Qué contiene el mensaje Identify

El protocolo Identify intercambia información que incluye [3](#4-2) :

- `listenAddrs`: Direcciones donde el peer está escuchando
- `protocols`: Protocolos soportados por el peer
- `agentVersion`: Versión del cliente
- `observedAddr`: Dirección observada del peer que llama

### Ejemplo de Test

Puedes ver cómo funciona en los tests oficiales [4](#4-3) :

```kotlin
val identify = Identify().dial(clientHost, serverHost.peerId, Multiaddr(listenAddress))
val remoteIdentity = identifyController.id().get(5, TimeUnit.SECONDS)

// Las direcciones reales están en listenAddrsList
val remoteAddress = Multiaddr.deserialize(remoteIdentity.listenAddrsList[0].toByteArray())
assertEquals(listenAddress, remoteAddress.toString())
```

## Consideraciones Importantes

1. **Dos llamadas necesarias**: Es el patrón estándar en libp2p
2. **Multiplexing**: Ambos protocolos pueden usar la misma conexión TCP
3. **AddressBook**: Considera almacenar las direcciones obtenidas para evitar repetir el proceso
4. **Timing**: Asegúrate de que la conexión esté completamente establecida antes de llamar a
   `remoteAddress()`

## Para tu Sistema de Discovery

1. Ejecuta `Identify` en cada nueva conexión
2. Extrae y almacena las direcciones reales en tu AddressBook
3. Usa estas direcciones para futuras conexiones automáticas
4. Implementa cache para evitar llamadas repetidas de `Identify`

## Versión y Compatibilidad

- **jvm-libp2p**: 1.2.0-RELEASE
- **Java**: 21
- **Protocolo Identify**: `/ipfs/id/1.0.0`

---

*Este documento está basado en el análisis del código fuente de libp2p/jvm-libp2p y los patrones de
uso en los tests oficiales.*

Wiki pages you might want to explore:

- [Overview (libp2p/jvm-libp2p)](/wiki/libp2p/jvm-libp2p#1)
- [Architecture Overview (libp2p/jvm-libp2p)](/wiki/libp2p/jvm-libp2p#1.1)
