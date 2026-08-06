# Orion Scripts (Windows PowerShell)

Generado a partir de los scripts para pruebas en local de los nodos.

Este documento explica cómo usar los scripts PowerShell de Windows incluidos en este proyecto. Ignora scripts de Linux.

Incluye:
- orion-start-node.ps1: inicia un único nodo Orion.
- orion-start-pool.ps1: inicia varios nodos en segundo plano (pool).
- orion-stop-pool.ps1: detiene los nodos del pool.

Fecha: 2025-09-03


## 1) Prerrequisitos
- Windows con PowerShell (5.x o 7.x).
- Java instalado (java disponible en PATH).
- El JAR de la app compilado en target/orion-application-1.0.0-SNAPSHOT.jar
  - Si no existe, ejecuta: mvn -q -DskipTests package


## 2) Política de ejecución (si hace falta)
Si PowerShell bloquea la ejecución de scripts, puedes habilitar la ejecución durante la sesión actual:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```


## 3) orion-start-node.ps1 (un solo nodo)
Inicia un nodo de Orion en la consola actual. Todos los parámetros son opcionales y tienen valores por defecto.

Parámetros (opcionales):
- -ORION_FILES <string>  Carpeta a escanear por Orion (por defecto: %USERPROFILE%\Downloads\ORION-FILES\share2)
- -ORION_PORT <string>   Puerto P2P del nodo (por defecto: 5051)
- -ORION_BOOTSTRAP_IP <string>   IP del bootstrap (por defecto: 127.0.0.1)
- -ORION_BOOTSTRAP_PORT <string> Puerto del bootstrap (por defecto: 4050)
- -ORION_BOOTSTRAP_ID <string>   PeerId del bootstrap (por defecto: QmSzM1...FEUd)
- -SPRING_PROFILE <string>       Perfil Spring (por defecto: pro)
- -SPRING_DATASOURCE_URL <string> URL JDBC. Si no lo indicas, usa jdbc:sqlite:application{ORION_PORT}.db en el directorio actual.

Ejemplos:
```powershell
# Usar todos los valores por defecto
.\orion-start-node.ps1

# Con carpeta y puerto personalizados
.\orion-start-node.ps1 -ORION_FILES "C:\DATA\ORION-FILES\shareA" -ORION_PORT 5055

# Definir bootstrap y datasource explícitos
.\orion-start-node.ps1 -ORION_BOOTSTRAP_IP 192.168.1.10 -ORION_BOOTSTRAP_PORT 4050 -ORION_BOOTSTRAP_ID 'Qm...' -SPRING_DATASOURCE_URL "jdbc:sqlite:C:\DATA\ORION-FILES\datasource\application5055.db"
```

Notas:
- El script muestra por consola la configuración efectiva antes de lanzar Java.
- El JAR que se ejecuta es: target/orion-application-1.0.0-SNAPSHOT.jar


## 4) orion-start-pool.ps1 (múltiples nodos en segundo plano)
Lanza N instancias del nodo como procesos en segundo plano. Requiere ID de bootstrap y el número de instancias.

Parámetros:
- -ORION_BOOTSTRAP_ID <string> (alias: -b)  Obligatorio. PeerId del bootstrap.
- -Instances <int> (alias: -i)              Obligatorio. Número de instancias a lanzar (>=1).
- -ORION_FILES <string>                     Opcional. Carpeta base para recursos (por defecto: %USERPROFILE%\Downloads\ORION-FILES).
- -ORION_BOOTSTRAP_IP <string>              Opcional. IP del bootstrap (por defecto: 127.0.0.1).
- -ORION_BOOTSTRAP_PORT <string>            Opcional. Puerto del bootstrap (por defecto: 4050).
- -SPRING_PROFILE <string>                  Opcional. Perfil Spring (por defecto: pro).
- -BasePort <int>                           Opcional. Puerto inicial para la primera instancia (por defecto: 5050). Los siguientes serán 5051, 5052, ...

Comportamiento:
- Para cada instancia k (empezando en 0):
  - ORION_PORT = BasePort + k
  - SPRING_DATASOURCE_URL = jdbc:sqlite:{ORION_FILES}\datasource\application{ORION_PORT}.db
  - SHARED_FOLDER = {ORION_FILES}\pool\share-{k}
- Crea automáticamente las carpetas base si no existen: {ORION_FILES}, {ORION_FILES}\datasource, {ORION_FILES}\pool.
- Lanza cada instancia mediante orion-start-node.ps1 en segundo plano.
- Logs por instancia: {ORION_FILES}\logs\node{X}.log (X = 1..Instances)
  - Asegúrate de que exista la carpeta {ORION_FILES}\logs (si no existe, créala antes de lanzar el pool).

Ejemplos:
```powershell
# Lanzar 3 instancias en puertos 5050..5052, con ID de bootstrap
.\orion-start-pool.ps1 -ORION_BOOTSTRAP_ID 'Qm...' -i 3

# Cambiar carpeta base y puerto inicial
.\orion-start-pool.ps1 -b 'Qm...' -i 2 -ORION_FILES 'C:\DATA\ORION-FILES' -BasePort 6000

# Perfil distinto
.\orion-start-pool.ps1 -b 'Qm...' -i 4 -SPRING_PROFILE local
```

Consejos:
- Verifica que el JAR exista: target/orion-application-1.0.0-SNAPSHOT.jar
- Crea la carpeta de logs si no existe:
  ```powershell
  New-Item -ItemType Directory -Force -Path "C:\DATA\ORION-FILES\logs" | Out-Null
  ```


## 5) orion-stop-pool.ps1 (detener nodos del pool)
Detiene procesos java.exe de Orion y, si aplica, los wrappers powershell.exe usados para iniciarlos.

Parámetros:
- -BasePort <int>     Opcional. Puerto base del pool que quieres detener (para filtrar por puertos).
- -Instances <int>    Opcional. Número de instancias (para calcular el rango de puertos BasePort..BasePort+Instances-1).
- -DryRun             Opcional. Muestra qué se detendría sin matar procesos.

Uso típico:
```powershell
# Parar todo lo detectado (sin filtros)
.\orion-stop-pool.ps1

# Ver únicamente qué se pararía
.\orion-stop-pool.ps1 -DryRun

# Parar solo un pool específico (ej.: BasePort 5050, 3 instancias -> 5050..5052)
.\orion-stop-pool.ps1 -BasePort 5050 -Instances 3
```

Cómo funciona:
- Busca procesos java.exe cuyo CommandLine contenga el JAR: orion-application-1.0.0-SNAPSHOT.jar
- Si das -BasePort y -Instances, filtra además por el argumento JVM -Dorion.p2p.port={puerto}.
- Luego, intenta detener wrappers de PowerShell que hayan lanzado orion-start-node.ps1.


## 6) Solución de problemas
- "El término .ps1 no se puede ejecutar": establece execution policy en la sesión:
  ```powershell
  Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
  ```
- "No se encontró el JAR": compila el proyecto:
  ```powershell
  mvn -q -DskipTests package
  ```
- Los logs no aparecen: confirma que existe la carpeta {ORION_FILES}\logs y que tienes permisos de escritura.
- Puertos ocupados: cambia -BasePort o detén procesos anteriores con orion-stop-pool.ps1.
- Rutas con espacios: si pasas rutas en parámetros, ponlas entre comillas dobles.


## 7) Resumen rápido
- 1 nodo: `.\orion-start-node.ps1`
- N nodos en background: `.\orion-start-pool.ps1 -b 'Qm...' -i 3`
- Pararlos: `.\orion-stop-pool.ps1` o selectivo con `-BasePort` y `-Instances`
