
param(

    [Parameter(Mandatory = $false)]
    [Alias("d")]
    [string]$ORION_USERPROFILE = "", #primary config

    [Parameter(Mandatory = $false)]
    [Alias("p")]
    [string]$ORION_PORT = "5051",

    [Parameter(Mandatory = $false)]
    [string]$ORION_BOOTSTRAP_IP = "127.0.0.1",

    [Parameter(Mandatory = $false)]
    [string]$ORION_BOOTSTRAP_PORT = "4050",

    [Parameter(Mandatory = $false)]
    [Alias("b")]
    [string]$ORION_BOOTSTRAP_ID = "QmSzM1Cj2h58Wvn1b3JvXEZ3oi72iG4UhxNperPWB7FEUd",

    [Parameter(Mandatory = $false)]
    [string]$SPRING_PROFILE = "pro",

    [Parameter(Mandatory = $false)]
    [string]$SPRING_DATASOURCE_URL,

    [Parameter(Mandatory = $false, HelpMessage = "Arranca el nodo SIN bootstrap en la BD (tabla vacía) para gestionarlos desde la UI")]
    [switch]$NoBootstrap
)

if (-not $SPRING_DATASOURCE_URL -or [string]::IsNullOrWhiteSpace($SPRING_DATASOURCE_URL)) {
    $SPRING_DATASOURCE_URL = "jdbc:sqlite:application$ORION_PORT.db"
}

# Mostramos las variables por consola para verificar la configuración
Write-Host "Configuration:"
Write-Host "`tORION_USERPROFILE`: $ORION_USERPROFILE"
# Write-Host "`tORION_FILES: $ORION_FILES"
# Write-Host "`tORION_TMP: $ORION_TMP"
# Write-Host "`tORION_DOWNLOAD: $ORION_DOWNLOAD"
Write-Host "`tORION_PORT: $ORION_PORT"
Write-Host "`tORION_BOOTSTRAP_IP: $ORION_BOOTSTRAP_IP"
Write-Host "`tORION_BOOTSTRAP_PORT: $ORION_BOOTSTRAP_PORT"
Write-Host "`tORION_BOOTSTRAP_ID: $ORION_BOOTSTRAP_ID"
Write-Host "`tSPRING_PROFILE: $SPRING_PROFILE"
Write-Host "`tSPRING_DATASOURCE_URL: $SPRING_DATASOURCE_URL"
Write-Host "`tNO_BOOTSTRAP: $NoBootstrap"


# ---------------------------------------------------------------------------
# Fase 1: siembra de la BD del nodo ANTES de arrancar.
# El nodo lee el puerto de escucha SOLO de NODE_CONFIG y los bootstraps
# SOLO de la tabla BOOTSTRAP (la BD es la unica fuente). El script manda:
# con sqlite3.exe escribe el puerto y (si no es -NoBootstrap) el bootstrap
# en la BD antes de que el JVM abra el fichero.
# Con -NoBootstrap la tabla BOOTSTRAP se deja VACIA a proposito (para
# gestionarla desde la UI).
# Se conserva -Dorion.p2p.port en el JVM porque orion-stop-pool.ps1
# filtra procesos java por ese parametro.
# ---------------------------------------------------------------------------
$dbFile = $SPRING_DATASOURCE_URL -replace "^jdbc:sqlite:", ""

$portInt = 0
if (-not [int]::TryParse($ORION_PORT, [ref]$portInt) -or $portInt -lt 1 -or $portInt -gt 65535) {
    Write-Error "ORION_PORT inválido (1-65535): '$ORION_PORT'"
    exit 1
}

# Localizar sqlite3.exe: primero la raíz del proyecto (padre de esta carpeta), luego el PATH.
$sqlite3 = $null
foreach ($candidate in @("$PSScriptRoot\sqlite3.exe", "$PSScriptRoot\..\sqlite3.exe")) {
    if (Test-Path $candidate) { $sqlite3 = (Resolve-Path $candidate).Path; break }
}
if (-not $sqlite3) {
    $cmd = Get-Command sqlite3 -ErrorAction SilentlyContinue
    if ($cmd) { $sqlite3 = $cmd.Source }
}
if (-not $sqlite3) {
    Write-Error "sqlite3 no encontrado (esperado en la raíz del proyecto o en el PATH). No puedo sembrar la BD: $dbFile"
    exit 1
}

$dbDir = Split-Path -Parent $dbFile
if ($dbDir -and -not (Test-Path $dbDir)) {
    New-Item -ItemType Directory -Path $dbDir -Force | Out-Null
}

$sql = @"
CREATE TABLE IF NOT EXISTS NODE_CONFIG(
    ID INTEGER PRIMARY KEY,
    PORT INTEGER NOT NULL DEFAULT 4050,
    LIMIT_K INTEGER NOT NULL DEFAULT 20
);
INSERT INTO NODE_CONFIG (ID, PORT, LIMIT_K) VALUES (1, $portInt, 20)
    ON CONFLICT(ID) DO UPDATE SET PORT=excluded.PORT, LIMIT_K=excluded.LIMIT_K;
CREATE TABLE IF NOT EXISTS BOOTSTRAP(
    ID INTEGER PRIMARY KEY,
    IP TEXT NOT NULL,
    PORT TEXT NOT NULL,
    PEER_ID TEXT NOT NULL,
    CONSTRAINT unique_bootstrap_peer UNIQUE (PEER_ID)
);
DELETE FROM BOOTSTRAP;
"@
$bootstrapDesc = ""
if (-not $NoBootstrap) {
    $sql += "INSERT INTO BOOTSTRAP (IP, PORT, PEER_ID) VALUES ('$ORION_BOOTSTRAP_IP', '$ORION_BOOTSTRAP_PORT', '$ORION_BOOTSTRAP_ID');"
    $bootstrapDesc = "bootstrap=${ORION_BOOTSTRAP_IP}:${ORION_BOOTSTRAP_PORT} id=$ORION_BOOTSTRAP_ID"
} else {
    $bootstrapDesc = "SIN BOOTSTRAP (tabla vacia a proposito)"
}

& $sqlite3 $dbFile $sql
if ($LASTEXITCODE -ne 0) {
    Write-Error "sqlite3 falló (código $LASTEXITCODE) al sembrar $dbFile"
    exit 1
}
Write-Host "`tBD sembrada: $dbFile (PORT=$portInt, LIMIT_K=20, $bootstrapDesc)" -ForegroundColor DarkCyan
# ---------------------------------------------------------------------------

# Comando para inicializar el nodo de Orion
# $env:USERPROFILE = $ORION_USERPROFILE
# Nota: -Dorion.p2p.port se conserva porque orion-stop-pool.ps1 filtra los
# procesos java por ese parametro.
java "-Dspring.profiles.active=$SPRING_PROFILE" `
     "-Dspring.datasource.url=$SPRING_DATASOURCE_URL" `
     "-Dprism.order=sw" `
     "-Djavafx.headless=true" `
     "-Dglass.platform=Monocle" `
     "-Dmonocle.platform=Headless" `
     "-Dorion.user-profile=$ORION_USERPROFILE" `
     "-Dorion.p2p.limitK=20" `
     "-Dorion.p2p.port=$ORION_PORT" `
     -jar "target/orion-application-1.0.0-SNAPSHOT.jar" --enabled-ui="false"

