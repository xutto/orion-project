# Patrón Event-Driven Command — Orion UI

## Resumen

La UI de Orion usa un patrón **Command + Event Bus** personalizado que desacopla por completo la lógica de los controllers JavaFX. Los controllers solo registran nodos; la lógica vive en comandos Spring beans.

---

## Flujo de arranque (2 fases)

En `DesktopApplicationLoader.start()`, dentro de `stage.setOnShown()`:

### Fase 1 — Declaración de compatibilidades
```java
context.getBeansOfType(Command.class)
    .forEach((name, bean) -> bean.configureCompatibility());
```
Spring itera todos los beans que implementan `Command<T extends Event>`. Cada uno llama a `configureCompatibility()` y rellena su mapa interno:
```java
Map<EventType, List<Node>> compatibilities;
```

En esta fase el comando dice: *"me interesa MouseEvent en estos 3 iconos y en el pane principal"*. Los Node los obtiene de `ControllerNodes` por ID constante.

### Fase 2 — Asignación de listeners
```java
context.getBeansOfType(Events.class)
    .forEach((e, t) -> t.configure());
```
Esto invoca `EventsConfiguratorImpl.configure()`, que a su vez llama a `EventsAssemblerImpl.configureCommandEvents(commands)`. El assembler recibe **todos** los comandos, lee sus compatibilidades y asigna los listeners JavaFX reales (`setOnMouseClicked`, `addEventFilter`, etc).

### Fase 3 — Ejecución en tiempo real
Cuando el usuario hace clic en un nodo, el assembler ya montado ejecuta:
```java
n.setOnMouseClicked(mouseEvent ->
    determineCommandsByNode(eventCommands, n, eventType)
        .forEach(c -> c.execute(mouseEvent)));
```
`determineCommandsByNode` itera **todos** los comandos, filtra por EventType y por nodeId, y ejecuta cada uno. Un solo clic dispara N comandos en paralelo.

---

## Componentes del motor de eventos

| Clase | Paquete | Rol |
|---|---|---|
| `Command<T extends Event>` | `command/` | Interface base. Define `getCompatibilities()`, `configureCompatibility()`, `execute(T event)`, `execute()` |
| `EventType` | `events/` | Enum: `ON_BOOT`, `MOUSE_CLICKED`, `KEY_RELEASED`, `ON_HOVER`, `ON_ACTION` |
| `Events` (interface) | `events/` | Contrato con método `configure()`. Punto de entrada único. |
| `EventsConfiguratorImpl` | `events/` | Implementa `Events`. Inyecta todos los `Command<Event>` por Spring y se los pasa al assembler. |
| `EventsAssembler` | `events/` | Interface: `configureCommandEvents(List<Command<Event>>)`. |
| `EventsAssemblerImpl` | `events/` | **Núcleo del patrón**. Itera comandos, asigna handlers JavaFX, dispatch por nodeId. Usa virtual threads (`ExecutorService`). |
| `ControllerNodes` | `nodes/` | Registry `id → Node`. Cada controller lo inyecta y registra sus `@FXML` nodes aquí. Acceso tipado: `getNode(id, Class<T>)`. |

---

## Contrato del interface Command

```java
public interface Command<T extends Event> {

  // Devuelve el mapa de compatibilidades (EventType → nodos)
  Map<EventType, List<Node>> getCompatibilities();

  // Fase 1: rellena el mapa. Se llama una vez al arranque.
  void configureCompatibility();

  // Ejecuta cuando hay evento JavaFX (clic, key, hover…)
  default void execute(T event);

  // Ejecuta sin evento (ON_BOOT, tareas periódicas…)
  default void execute();
}
```

---

## Tipos de comandos según EventType

### ON_BOOT (sin nodo)
Se ejecutan en la fase 2 directamente desde el assembler:
```java
case EventType.ON_BOOT -> command.execute();
```
Ejemplos: `ScanFilesCommand`, `SearchFileCommand`. Suscriben publishers, inician watchers.

### MOUSE_CLICKED (con nodos específicos)
El assembler asigna un listener por nodo:
```java
nodes.forEach(n -> n.setOnMouseClicked(event ->
    determineCommandsByNode(allCommands, n, e).forEach(c -> c.execute(event))));
```
Un clic en el mismo nodo puede disparar múltiples comandos (cambia título + muestra pane + toggle tooltip).

### KEY_RELEASED (escenario global)
Se monta un event filter por teclado en la Scene:
```java
nodes.forEach(n -> n.getScene().addEventFilter(KeyEvent.KEY_RELEASED, ...));
```
Ejemplo: `SwitchSettingsVisibleCommand` responde a Escape.

---

## Reglas obligatorias

1. **IDs siempre por constantes** — Usar `NodesIdentifierConstants.XXX`. Nunca strings hardcoded en código Java. El `id` FXML debe coincidir con la constante.
2. **Un comando = una responsabilidad** — Cada comando hace algo específico. Si un clic necesita 3 efectos, monta 3 comandos. El dispatcher los ejecuta todos.
3. **Sin lógica de negocio en controllers** — Los controllers solo registran nodos en `ControllerNodes`. Si necesitas ejecutar algo al hacer clic, crea un `@Component Command<Event>`.
4. **Textos de UI en inglés** — Tooltips, labels, mensajes de usuario → inglés.

---

## Pitfalls conocidos

1. **Búsqueda O(N) por evento** — Cada clic itera TODOS los beans Command. Con ~15 comandos no se nota. Si crece, pre-computar índice `Map<NodeId, List<Command>>`.
2. **Configuración asíncrona sin barrier** — `EventsAssemblerImpl` usa `executor.submit()` virtual threads sin esperar completitud. Si el usuario hace clic antes de que se asignen los handlers, no pasa nada. Solución: agregar `CompletableFuture.allOf().join()`.
3. **HashSet no thread-safe** — `onMouseClickedNodesConfigured` es un `HashSet` accesible desde múltiples threads virtuales. Usar `ConcurrentHashMap.newKeySet()` si hay problemas concurrencia.
4. **KEY_RELEASED ejecuta todos** — El assembler no filtra por KeyCode; cada comando hace el check interno (`KeyEvent.getCode()`).
5. **Sin prioridad entre comandos** — Si dos comandos compiten (uno muestra X, otro lo oculta), el orden depende de Spring `getBeansOfType()`. Solución: implementar `Ordered` o campo `priority` en Command.
6. **Nodos configurados solo una vez** — El filter `.filter(n → !onMouseClickedNodesConfigured.contains(n))` previene re-asignar handlers al mismo nodo.
