param(
    [Parameter(Mandatory = $false, HelpMessage = "Puerto base desde el que se lanzaron las instancias (ej. 5050)")]
    [Alias("p")]
    [int]$BasePort = 5050,

    [Parameter(Mandatory = $false, HelpMessage = "Número de instancias lanzadas (ej. 3)")]
    [Alias("i")]
    [int]$Instances,

    [Parameter(Mandatory = $false)]
    [Alias("d")]
    [string]$ORION_BASE_FILES = "D:\Download\ORION-FILES",

    [Parameter(Mandatory = $false, HelpMessage = "Solo mostrar qué procesos se detendrían, sin matar")]
    [Alias("r")]
    [switch]$DryRun
)

# Configuración
$ErrorActionPreference = 'Stop'
$jarRegex = 'orion-application-1\.0\.0-SNAPSHOT\.jar'
$nodeScriptRegex = 'orion-start-node\.ps1'

function Get-OrionJavaProcesses {
    param(
        [int[]]$Ports
    )
    $procs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue
    if (-not $procs) { return @() }

    $procs = $procs | Where-Object { $_.CommandLine -match $jarRegex }

    if ($Ports -and $Ports.Count -gt 0) {
        $procs = $procs | Where-Object {
            $cmd = $_.CommandLine
            foreach ($p in $Ports) {
                if ($cmd -match "-Dorion\.p2p\.port=$p\b") { return $true }
            }
            return $false
        }
    }
    return @($procs) # garantizar array
}

function Get-OrionWrapperProcesses {
    $procs = Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" -ErrorAction SilentlyContinue
    if (-not $procs) { return @() }

    $procs = $procs | Where-Object { $_.CommandLine -match $nodeScriptRegex }
    return @($procs) # garantizar array
}

function Stop-Processes {
    param(
        [AllowNull()]
        [AllowEmptyCollection()]
        [array]$Processes = @(),

        [switch]$DryRun
    )
    if (-not $Processes -or $Processes.Count -eq 0) { return }

    foreach ($p in $Processes) {
        if ($DryRun) {
            Write-Host "[DRY-RUN] Would stop PID=$($p.ProcessId) : $($p.CommandLine)" -ForegroundColor Yellow
        } else {
            try {
                Stop-Process -Id $p.ProcessId -Force -ErrorAction Stop
                Write-Host "[OK] Stopped PID=$($p.ProcessId) : $($p.Name)" -ForegroundColor Green
            } catch {
                Write-Warning "[WARN] Could not stop PID=$($p.ProcessId): $($_.Exception.Message)"
            }
        }
    }
}

# Lógica de PIDs registrados
$pidFile = Join-Path -Path $ORION_BASE_FILES -ChildPath "pids.txt"

if (Test-Path $pidFile) {
    Write-Host "Procesando PIDs registrados en: $pidFile" -ForegroundColor Cyan
    $savedPids = Get-Content $pidFile | Where-Object { $_ -match '^\d+$' }

    foreach ($procId in $savedPids) {
        if ($DryRun) {
            Write-Host "[DRY-RUN] Se cerraría el proceso registrado con PID: $procId" -ForegroundColor Yellow
        } else {
            # 1. Verificamos si el proceso realmente existe antes de actuar
            if (Get-Process -Id $procId -ErrorAction SilentlyContinue) {
                # 2. Si existe, lo matamos de forma silenciosa
                taskkill /F /T /PID $procId 2>$null | Out-Null
                Write-Host "[OK] Señal de parada enviada a PID guardado: $procId" -ForegroundColor Green
            } else {
                # 3. Si no existe, simplemente informamos o ignoramos
                Write-Host "[INFO] El proceso con PID $procId ya no existe. Saltando..." -ForegroundColor Red
            }
        }
    }

    if (-not $DryRun) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Host "Archivo de PIDs limpiado." -ForegroundColor Gray
    }
}


# Determinar puertos si se indicó rango
[int[]]$ports = @()
if ($PSBoundParameters.ContainsKey('BasePort') -and $PSBoundParameters.ContainsKey('Instances')) {
    if ($Instances -lt 1) {
        Write-Error "Instances debe ser >= 1 si se especifica BasePort"; exit 1
    }
    $max = $BasePort + $Instances - 1
    $ports = $BasePort..$max
    Write-Host "Filtrando por puertos: $($ports -join ', ')" -ForegroundColor Cyan
} elseif ($PSBoundParameters.ContainsKey('BasePort') -or $PSBoundParameters.ContainsKey('Instances')) {
    Write-Warning "Para filtrar por puertos debes especificar ambos parámetros: -BasePort y -Instances. Se ignorará el filtro."
}

# Localizar procesos Java de Orion
$javaProcs = Get-OrionJavaProcesses -Ports $ports

if ($javaProcs -and $javaProcs.Count -gt 0) {
    Write-Host "Encontrados $($javaProcs.Count) proceso(s) java.exe de Orion." -ForegroundColor Cyan
} else {
    Write-Host "No se encontraron procesos java.exe de Orion activos." -ForegroundColor Yellow
}

# Localizar wrappers de PowerShell que lanzan el nodo
$wrapperProcs = Get-OrionWrapperProcesses
if ($wrapperProcs -and $wrapperProcs.Count -gt 0) {
    Write-Host "Encontrados $($wrapperProcs.Count) wrapper(s) powershell.exe." -ForegroundColor Cyan
} else {
    Write-Host "No se encontraron procesos powershell.exe wrappers de Orion." -ForegroundColor Yellow
}

# Si no hay nada que parar, salida bonita y exit 0
if ((!$javaProcs -or $javaProcs.Count -eq 0) -and (!$wrapperProcs -or $wrapperProcs.Count -eq 0)) {
    if ($DryRun) {
        Write-Host "`n[DRY-RUN] No hay procesos que se detendrían." -ForegroundColor Yellow
    } else {
        Write-Host "`nNo hay procesos en ejecución. Nada que detener." -ForegroundColor Yellow
    }
    exit 0
}

# Mostrar resumen si es DryRun
if ($DryRun) {
    if ($javaProcs -and $javaProcs.Count -gt 0) {
        Write-Host "`n[DRY-RUN] java.exe a detener:" -ForegroundColor Yellow
        $javaProcs | Select-Object ProcessId, CommandLine | Format-List | Out-String | Write-Host
    }
    if ($wrapperProcs -and $wrapperProcs.Count -gt 0) {
        Write-Host "`n[DRY-RUN] powershell.exe wrappers a detener:" -ForegroundColor Yellow
        $wrapperProcs | Select-Object ProcessId, CommandLine | Format-List | Out-String | Write-Host
    }
    Write-Host "`nSeco: no se detuvo ningún proceso." -ForegroundColor Yellow
    exit 0
}

# Primero parar java.exe (los procesos efectivos de la app)
if ($javaProcs -and $javaProcs.Count -gt 0) {
    Stop-Processes -Processes $javaProcs
} else {
    Write-Host "No hay java.exe de Orion que detener." -ForegroundColor DarkGray
}

# Luego, parar wrappers powershell.exe si quedaran
if ($wrapperProcs -and $wrapperProcs.Count -gt 0) {
    Stop-Processes -Processes $wrapperProcs
} else {
    Write-Host "No hay wrappers powershell.exe que detener." -ForegroundColor DarkGray
}

Write-Host "`nFinalizado orion-stop-pool.ps1" -ForegroundColor Green

Write-Host "`nEjemplos:" -ForegroundColor DarkCyan
Write-Host "  .\orion-stop-pool.ps1                           # Para todo Orion (java + wrappers) detectado" -ForegroundColor DarkCyan
Write-Host "  .\orion-stop-pool.ps1 -DryRun                  # Previsualiza sin matar" -ForegroundColor DarkCyan
Write-Host "  .\orion-stop-pool.ps1 -BasePort 5050 -Instances 3  # Solo puertos 5050..5052" -ForegroundColor DarkCyan
