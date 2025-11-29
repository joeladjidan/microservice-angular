# start-all.ps1
# Script PowerShell pour Windows : démarre les modules Java (mvn spring-boot:run) et le client Angular (npm start)
# Placez ce fichier à la racine du projet et lancez :
#   PowerShell -ExecutionPolicy Bypass -File .\start-all.ps1

param(
    [switch]$Docker,
    [switch]$SkipAngular,
+
    [switch]$BuildOnly,
    [int]$Port = 4200
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Path $MyInvocation.MyCommand.Definition -Parent
$LogsDir = Join-Path $Root 'logs'
if (-not (Test-Path $LogsDir)) { New-Item -ItemType Directory -Path $LogsDir | Out-Null }

$Modules = @('api-gateway','customer-service','order-service','file-processor')

function Run-CommandInBackground {
    param(
        [string]$WorkingDir,
        [string]$Command,
        [string]$LogFile
    )
    $StartInfo = New-Object System.Diagnostics.ProcessStartInfo
    $StartInfo.FileName = 'powershell.exe'
    $StartInfo.Arguments = "-NoProfile -Command \"Set-Location -LiteralPath '$WorkingDir'; $Command\""
    $StartInfo.RedirectStandardOutput = $true
    $StartInfo.RedirectStandardError = $true
    $StartInfo.UseShellExecute = $false
    $StartInfo.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $StartInfo
    $outStream = [System.IO.File]::OpenWrite($LogFile)
    $outStream.Seek(0, 'End') | Out-Null
    $proc.Start() | Out-Null
    $proc.Id | Out-Null
    # Note: redirect streams not hooked here to keep implementation simple
    return $proc.Id
}

if ($Docker) {
    Write-Host "Lancement via Docker Compose..."
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "docker introuvable" }
    Push-Location $Root
    docker compose up -d
    Pop-Location
    return
}

if ($BuildOnly) {
    Write-Host "Packaging modules Java (skip tests)..."
    foreach ($m in $Modules) {
        $pom = Join-Path $Root $m\'pom.xml'
        if (Test-Path $pom) {
            Write-Host "Packaging $m..."
            Push-Location (Join-Path $Root $m)
            mvn -DskipTests package
            Pop-Location
        } else { Write-Host "Ignore $m (pas de pom.xml)" }
    }
    return
}

$Pids = @()
foreach ($m in $Modules) {
    $pom = Join-Path $Root $m\'pom.xml'
    if (Test-Path $pom) {
        $log = Join-Path $LogsDir ("$m.log")
        Write-Host "Démarrage de $m (logs: $log)"
        $cmd = "mvn -Dspring-boot.run.fork=false -DskipTests spring-boot:run"
        $id = Start-Process -FilePath 'powershell.exe' -ArgumentList "-NoProfile -Command \"Set-Location -LiteralPath '$Root\$m'; $cmd | Out-File -FilePath '$log' -Encoding utf8\"" -WindowStyle Hidden -PassThru
        $Pids += $id.Id
    } else { Write-Host "Module $m ignoré (pas de pom.xml)" }
}

if (-not $SkipAngular) {
    # support both 'portail-ui' and legacy 'client-gateway'
    $ca = Join-Path $Root 'portail-ui'
    $clientName = 'portail-ui'
    if (-not (Test-Path $ca)) { $ca = Join-Path $Root 'client-gateway'; $clientName = 'client-gateway' }
    if (Test-Path $ca) {
        $log = Join-Path $LogsDir "$clientName.log"
        Write-Host "Démarrage du client Angular ($clientName) (logs: $log)"
        # installer dépendances si nécessaire
        Push-Location $ca
        if (Test-Path (Join-Path $ca 'yarn.lock') -and (Get-Command yarn -ErrorAction SilentlyContinue)) {
            Write-Host "Installing dependencies with yarn in $ca"
            Start-Process -FilePath 'yarn' -ArgumentList 'install' -WorkingDirectory $ca -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError $log -PassThru | Out-Null
        } else {
            Write-Host "Installing dependencies with npm in $ca"
            Start-Process -FilePath 'npm' -ArgumentList 'install' -WorkingDirectory $ca -NoNewWindow -RedirectStandardOutput $log -RedirectStandardError $log -PassThru | Out-Null
        }
        Pop-Location
        # Start the dev server from the client folder to avoid using --prefix
        # detect proxy config
        $proxyFile = Join-Path $ca 'proxy.conf.json'
        $proxyArg = ''
        if (Test-Path $proxyFile) { $proxyArg = '--proxy-config proxy.conf.json' }
        $startCmd = "Set-Location -LiteralPath '$ca'; npx ng serve --host 0.0.0.0 --port $Port $proxyArg | Out-File -FilePath '$log' -Encoding utf8"
        $id = Start-Process -FilePath 'powershell.exe' -ArgumentList '-NoProfile','-Command',$startCmd -WindowStyle Hidden -PassThru
        $Pids += $id.Id
    } else { Write-Host "client-gateway introuvable, skip..." }
} else {
    Write-Host "Angular skipped"
}

Write-Host "Processus lancés (PIDs): $($Pids -join ', ')"
Write-Host "Consultez les logs dans $LogsDir pour suivre les sorties des applications."
