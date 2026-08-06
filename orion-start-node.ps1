
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
    [string]$SPRING_DATASOURCE_URL
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


# java -Dorion.files.scan.folder=$${ORION_FILES} -Dspring.profiles.active=pro -Dprism.order=sw -Djavafx.headless=true -Dglass.platform=Monocle -Dmonocle.platform=Headless -Dorion.p2p.limitK=20 -Dorion.p2p.port=$${ORION_PORT} -Dorion.p2p.bootstrap-ip=$${ORION_BOOTSTRAP_IP} -Dorion.p2p.bootstrap-port=$${ORION_BOOTSTRAP_PORT} -Dorion.p2p.bootstrap-id=$${ORION_BOOTSTRAP_ID} -jar /app/orion-application.jar --enabled-ui="false"

# Comando para inicializar el nodo de Orion
# $env:USERPROFILE = $ORION_USERPROFILE
java "-Dspring.profiles.active=$SPRING_PROFILE" `
     "-Dspring.datasource.url=$SPRING_DATASOURCE_URL" `
     "-Dprism.order=sw" `
     "-Djavafx.headless=true" `
     "-Dglass.platform=Monocle" `
     "-Dmonocle.platform=Headless" `
     "-Dorion.user-profile=$ORION_USERPROFILE" `
     "-Dorion.p2p.limitK=20" `
     "-Dorion.p2p.port=$ORION_PORT" `
     "-Dorion.p2p.bootstrap-ip=$ORION_BOOTSTRAP_IP" `
     "-Dorion.p2p.bootstrap-port=$ORION_BOOTSTRAP_PORT" `
     "-Dorion.p2p.bootstrap-id=$ORION_BOOTSTRAP_ID" `
     -jar "target/orion-application-1.0.0-SNAPSHOT.jar" --enabled-ui="false"

