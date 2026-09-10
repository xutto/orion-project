param(
    [Parameter(Mandatory = $false, HelpMessage = "Identificador del nodo bootstrap (ORION_BOOTSTRAP_ID). Obligatorio salvo -NoBootstrap")]
    [Alias("b")]
    [string]$ORION_BOOTSTRAP_ID = "",

    [Parameter(Mandatory = $false, HelpMessage = "Arranca los nodos SIN bootstrap en la BD (tabla vacia) para gestionarlos desde la UI")]
    [switch]$NoBootstrap,

    [Parameter(Mandatory = $true)]
    [Alias("i")]
    [ValidateRange(1, 1000)]
    [int]$Instances,

    [Parameter(Mandatory = $false)]
    [Alias("d")]
    [string]$ORION_BASE_FILES = "D:\Download\ORION-FILES",

    [Parameter(Mandatory = $false)]
    [string]$ORION_BOOTSTRAP_IP = "127.0.0.1",

    [Parameter(Mandatory = $false)]
    [string]$ORION_BOOTSTRAP_PORT = "4050",

    [Parameter(Mandatory = $false)]
    [string]$SPRING_PROFILE = "pro",

    [Parameter(Mandatory = $false)]
    [Alias("p")]
    [int]$BasePort = 5050
)

$ORION_ARTIFACT_VERSION = "1.0.0-SNAPSHOT"

if (-not $NoBootstrap -and ([string]::IsNullOrWhiteSpace($ORION_BOOTSTRAP_ID))) {
    Write-Error "Indica -ORION_BOOTSTRAP_ID, o usa -NoBootstrap para arrancar sin bootstrap."
    exit 1
}

# Mostrar configuración general
Write-Host "Orion Pool Launcher" -ForegroundColor Cyan
Write-Host "----------------------------------------"
if ($NoBootstrap) {
    Write-Host "Mode:`t`t SIN BOOTSTRAP (tabla BOOTSTRAP vacia en cada nodo)"
} else {
    Write-Host "Bootstrap ID:`t $ORION_BOOTSTRAP_ID"
}
Write-Host "Instances (-i):`t $Instances"
Write-Host "Files base folder:`t $ORION_BASE_FILES"
Write-Host "Bootstrap IP:`t $ORION_BOOTSTRAP_IP"
Write-Host "Bootstrap Port:`t $ORION_BOOTSTRAP_PORT"
Write-Host "Spring profile:`t $SPRING_PROFILE"
Write-Host "Base port:`t $BasePort"
Write-Host "Jar:`t`t target/orion-application-$ORION_ARTIFACT_VERSION.jar"
Write-Host "----------------------------------------"

# Verifica que el JAR existe (avisa pero no detiene)
$jarPath = Join-Path -Path (Get-Location) -ChildPath "target/orion-application-$ORION_ARTIFACT_VERSION.jar"
if (-not (Test-Path $jarPath)) {
    Write-Warning "No se encontró el JAR en: $jarPath. Asegúrate de compilar el proyecto (mvn package)."
}

# Asegurar que existen las carpetas base requeridas
# foreach ($dir in @($ORION_BASE_FILES, "$ORION_BASE_FILES\datasource", "$ORION_BASE_FILES\pool")) {
#     if (-not (Test-Path -LiteralPath $dir)) {
#         New-Item -ItemType Directory -Path $dir -Force | Out-Null
#         Write-Host "Creada carpeta: $dir" -ForegroundColor DarkCyan
#     }
# }
foreach ($dir in @("$ORION_BASE_FILES\pool")) {
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "Creada carpeta: $dir" -ForegroundColor DarkCyan
    }
}

for ($n = 0; $n -lt $Instances; $n++) {
    $ORION_USERPROFILE = "$ORION_BASE_FILES\pool\$n"
    $ORION_PORT = $BasePort + $n
    $SPRING_DATASOURCE_URL = "jdbc:sqlite:$ORION_USERPROFILE\application$ORION_PORT.db"
    $LOGS_FOLDER = "$ORION_BASE_FILES\logs"

    # Asegurar que existe el directorio compartido de esta instancia
#     if (-not (Test-Path -LiteralPath $SHARED_FOLDER)) {
#         New-Item -ItemType Directory -Path $SHARED_FOLDER -Force | Out-Null
#         Write-Host "\tCreado SHARED_FOLDER: $SHARED_FOLDER" -ForegroundColor DarkCyan
#     }
#     if (-not (Test-Path -LiteralPath $TMP_FOLDER)) {
#         New-Item -ItemType Directory -Path $TMP_FOLDER -Force | Out-Null
#         Write-Host "\tCreado TMP_FOLDER: $TMP_FOLDER" -ForegroundColor DarkCyan
#     }
#     if (-not (Test-Path -LiteralPath $DOWNLOAD_FOLDER)) {
#         New-Item -ItemType Directory -Path $DOWNLOAD_FOLDER -Force | Out-Null
#         Write-Host "\tCreado DOWNLOAD_FOLDER: $DOWNLOAD_FOLDER" -ForegroundColor DarkCyan
#     }

    if (-not (Test-Path -LiteralPath $ORION_USERPROFILE)) {
        New-Item -ItemType Directory -Path $ORION_USERPROFILE -Force | Out-Null
        Write-Host "Creado ORION_USERPROFILE: $ORION_USERPROFILE" -ForegroundColor DarkCyan
    }


    Write-Host "Lanzando instancia #$($n+1) en puerto $ORION_PORT" -ForegroundColor Green

    # Ejecutar el script del nodo en segundo plano, redirigiendo logs a nodeX.log #Join-Path -Path (Get-Location) -ChildPath "
    $nodeIndex = $n + 1
    $logPath = "$LOGS_FOLDER\node$nodeIndex.log"
    $logPath_error = "$LOGS_FOLDER\node_error$nodeIndex.log"

    $nodeScript = Join-Path -Path (Get-Location) -ChildPath "orion-start-node.ps1"
    # Nota: en modo -NoBootstrap NO se pasan -ORION_BOOTSTRAP_* vacios:
    # Start-Process (PS 5.1) rechaza un ArgumentList con cadenas vacias.
    # El script del nodo los ignora igualmente en ese modo.
    $argList = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $nodeScript,
        "-ORION_USERPROFILE", $ORION_USERPROFILE,
        "-ORION_PORT", $ORION_PORT.ToString(),
        "-SPRING_PROFILE", $SPRING_PROFILE,
        "-SPRING_DATASOURCE_URL", $SPRING_DATASOURCE_URL
    )
    if ($NoBootstrap) {
        $argList += "-NoBootstrap"
    } else {
        $argList += @("-ORION_BOOTSTRAP_IP", $ORION_BOOTSTRAP_IP,
                      "-ORION_BOOTSTRAP_PORT", $ORION_BOOTSTRAP_PORT,
                      "-ORION_BOOTSTRAP_ID", $ORION_BOOTSTRAP_ID)
    }

    $proc = Start-Process -FilePath "powershell.exe" -ArgumentList $argList -WindowStyle Hidden -RedirectStandardOutput $logPath -RedirectStandardError $logPath_error -PassThru
    if ($proc) {
        $proc.Id | Out-File -FilePath "$ORION_BASE_FILES\pids.txt" -Append
        Write-Host "process pid: $($proc.Id)"
    }
}

Write-Host "\nSe han lanzado $Instances instancia(s) en segundo plano. Revisa los logs nodeX.log en el directorio del proyecto." -ForegroundColor Cyan

# Consejos de uso
Write-Host "\nEjemplos de uso:" -ForegroundColor Yellow
Write-Host "  .\\orion-start-pool.ps1 -ORION_BOOTSTRAP_ID 'Qm...' -i 3"
Write-Host "  .\\orion-start-pool.ps1 -ORION_BOOTSTRAP_ID 'Qm...' -i 2 -ORION_BASE_FILES 'C:\\DATA\\ORION-FILES' -BasePort 5050 -SPRING_PROFILE pro"
Write-Host "  .\\orion-start-pool.ps1 -NoBootstrap -i 2 -BasePort 5050   # nodos sin bootstrap (gestionarlos desde la UI)"
