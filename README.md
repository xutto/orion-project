# Orion

Orion is a **decentralized peer-to-peer file sharing application** built on top of `jvm-libp2p` as its networking engine, Spring Boot for the business layer, and JavaFX for the desktop UI.

## Architecture

```
┌──────────────────────────────────────────────────────┐
│  Desktop Application Loader (Application.java)       │
│                                                      │
│  ┌─────────────────┐    ┌─────────────────────────┐  │
│  │   Spring Boot    │    │       JavaFX UI          │  │
│  │                 │    │                          │  │
│  │ Domain Services │◄──►│ Controllers + FXML Views │  │
│  │ P2P Protocol    │    │ Event-Driven Commands    │  │
│  │ Persistence     │    │                          │  │
│  └─────────────────┘    └─────────────────────────┘  │
│                                                      │
│                   jvm-libp2p engine                  │
└──────────────────────────────────────────────────────┘
```

### Stack

| Layer           | Technology                                   |
|-----------------|----------------------------------------------|
| Language        | Java 25                                      |
| Framework       | Spring Boot 3.2.5                            |
| UI              | JavaFX 23 + MaterialFX 11.17                 |
| P2P Engine      | jvm-libp2p (custom fork v1.2.2)             |
| Database        | SQLite + Hibernate JPA                       |
| Search          | Apache Lucene 10.2                           |
| Serialization   | Protocol Buffers                             |
| Crypto          | Bouncy Castle                                |
| Build           | Maven                                        |
| Container       | Docker                                       |

## Prerequisites

- **JDK 25** or later
- **Maven 3.x**
- **Docker & Docker Compose** (for multi-node local testing)

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd orion-application
```

### 2. Install Manual Dependencies

The following library is **not available in public Maven repositories** and must be installed manually into your local Maven repository:

#### `jvm-libp2p` (mandatory)

A custom fork of jvm-libp2p is required to compile Orion:

```bash
mvn install:install-file \
  -Dfile="/path/to/jvm-libp2p-1.2.2-RELEASE.jar" \
  -DgroupId="io.libp2p" \
  -DartifactId="jvm-libp2p" \
  -Dversion="1.2.2-custom-xutto" \
  -Dpackaging="jar"
```

#### `noise-java` (optional — recommended for local development)

```bash
mvn install:install-file \
  -Dfile="/path/to/noise-java-1.0.jar" \
  -DgroupId="com.southerstorm" \
  -DartifactId="noise-java" \
  -Dversion="1.0" \
  -Dpackaging="jar"
```

### 3. Configure Maven Repositories

Add the following repositories to your `~/.m2/settings.xml`:

```xml
<settings>
  <profiles>
    <profile>
      <id>orion-repos</id>
      <repositories>
        <repository>
          <id>jitpack.io</id>
          <url>https://jitpack.io</url>
        </repository>
        <repository>
          <id>dl.cloudsmith.libp2p</id>
          <url>https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/</url>
        </repository>
        <repository>
          <id>dl.cloudsmith.consensys</id>
          <url>https://dl.cloudsmith.io/public/consensys/maven/maven/</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>orion-repos</activeProfile>
  </activeProfiles>
</settings>
```

### 4. Build

```bash
mvn clean package
```

## Running the Application

### With UI (Default)

```bash
java -jar target/orion-application-1.0.0-SNAPSHOT.jar --enabled-ui=true
```

### Without UI (Headless / Server Mode)

```bash
java -jar target/orion-application-1.0.0-SNAPSHOT.jar --enabled-ui=false
```

## Running Multi-Node Tests Locally

Orion includes scripts and Docker configurations to spin up a **multi-node P2P network** for local testing and development.

### Prerequisites

- Docker and Docker Compose installed
- The project built with `mvn clean package`

### Starting Bootstrap Nodes (No UI)

Use the `orion-start-pool.sh` script to launch headless nodes that simulate separate LANs connected via a shared public network:

```bash
# Start 3 headless nodes
./orion-start-pool.sh -s -i 3

# Stop all running nodes
./orion-start-pool.sh -x
```

The script dynamically creates Docker networks to simulate each node being on a different LAN, all connected through a shared public network. Nodes are configured in **headless mode** (`--enabled-ui=false`).

### Starting the Main Node (With UI)

After the headless pool is running, start the main application with the UI and point it at the bootstrap node:

```bash
java \
  -Dorion.p2p.bootstrap-ip=172.20.0.3 \
  -Dorion.p2p.bootstrap-port=4002 \
  -jar target/orion-application-1.0.0-SNAPSHOT.jar --enabled-ui=true
```

The main node will discover and connect to the pool of headless nodes through the bootstrap peer.

### Docker Compose (Alternative)

For a predefined 3-node setup:

```bash
docker-compose up --build
```

## UI Development — SceneBuilder Compatibility

FXML files use **classpath-absolute paths** (`/ui/connection.fxml`) for image sources and `fx:include` references, which is required at runtime by the JavaFX application.

Before opening a FXML file in **SceneBuilder**, convert the paths to relative format using the project's converter script located at the parent directory:

```bash
# From the root project folder
.\FXMLBatchConverter.ps1 -mode "SceneBuilder"
```

After making changes in SceneBuilder, convert back:

```bash
.\FXMLBatchConverter.ps1 -mode "Application"
```

Failure to convert will cause SceneBuilder to report missing resources or broken includes.

## Architecture — Spring Boot / JavaFX Integration

Orion uses a custom **event-driven Command pattern** to cleanly separate the Spring Boot backend from the JavaFX UI layer, avoiding direct controller coupling.

### How It Works

1. **`DesktopApplicationLoader`** extends `javafx.application.Application` and bootstraps both Spring Boot and JavaFX in sequence.
2. On window shown, it collects all beans of type `Command`, `Events`, and `Creator` from the Spring context.
3. Each `Command<T extends Event>` declares **compatibilities**: a mapping of `EventType` → `List<Node>`.
4. The `EventsConfiguratorImpl` iterates over all commands and wires each node to its event handler via `EventsAssemblerImpl`.
5. When an event fires (e.g., `MOUSE_CLICKED`, `ON_BOOT`), the assembler finds matching commands and executes them.

This means **no controller directly calls a service**. Logic lives in Commands, and UI bindings are declarative.

### Event Types

| Type          | Trigger                                          |
|---------------|--------------------------------------------------|
| `ON_BOOT`     | Application startup (no event argument)         |
| `MOUSE_CLICKED` | Mouse click on a registered node               |
| `KEY_RELEASED`  | Any key release on the scene                   |

### Example — Adding a New Command

```java
@Slf4j
@RequiredArgsConstructor
@Component
public class ShowGreetingCommand implements Command<Event> {

    private final ControllerNodes homeControllerNodes;
    private final Map<EventType, List<Node>> compatibilities = new HashMap<>();

    @Override
    public Map<EventType, List<Node>> getCompatibilities() {
        return compatibilities;
    }

    @Override
    public void configureCompatibility() {
        // On boot: log a message (no node required)
        compatibilities.put(EventType.ON_BOOT, List.of());

        // On button click: show an alert
        compatibilities.put(EventType.MOUSE_CLICKED,
            List.of(homeControllerNodes.getNode("btnGreet")));
    }

    @Override
    public void execute() {
        log.info("Hello from Orion!");
    }

    @Override
    public void execute(Event event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Orion");
        alert.setHeaderText(null);
        alert.setContentText("Hello, world!");
        alert.showAndWait();
    }
}
```

Register the node ID (`btnGreet`) in your FXML and the command is wired automatically. No `setOnAction` needed.

## P2P Communication — Protocol Buffers

All inter-node communication uses **Protocol Buffers** for message serialization. Schema definitions live in `src/main/proto/` and are compiled automatically by the `protobuf-maven-plugin` during the build.

### Message Schemas

| File | Purpose |
|------|---------|
| `Peer.proto` | Peer identity and address information (`PeerData`, `PeerAddress`, `PeerSharer`) |
| `Discovery.proto` | KAD peer discovery (request → response with closest peers) |
| `Manifest.proto` | Shared file manifest (`FileManifest` with hash, names, size, sharing peers) |
| `Search.proto` | File search request (query snippet, depth, limit, originating peer) |
| `SearchResult.proto` | Search response containing matching file manifests |
| `FileTransfer.proto` | Chunked file transfer protocol (`FILE_GET`, `FILE_AVAILABILITY`, `FILE_CHUNK`, `TRANSFER_END`, `FILE_ERROR`) |

### Generated Objects

The `protoc` compiler generates Java classes in the `com.mac.orion.infrastructure.p2p.model` package. These are consumed by the P2P infrastructure layer:

```
infrastructure/p2p/
├── sender/          # Serializes and sends protobuf messages
├── receiver/        # Listens for incoming protobuf messages
├── handler/         # Deserializes and routes messages to domain logic
├── decoder/         # Custom protobuf decoder for libp2p streams
├── factory/         # Protocol factory per message type
└── protocol/        # Protocol definitions registered with libp2p
```

### Adding a New Message Type

1. Create or edit a `.proto` file in `src/main/proto/`.
2. Run `mvn clean package` — the plugin generates the Java classes automatically.
3. Implement a handler, sender, and receiver in the P2P layer.
4. Register the protocol in libp2p through a factory class.

## License

MIT License — Copyright (c) 2026 Jesús Angel Luque Ares.

See [LICENSE](LICENSE) for details.
