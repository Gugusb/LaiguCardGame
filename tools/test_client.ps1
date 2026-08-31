# ============================================================================
# test_client.ps1 - Laigu (Laigu) standardized test launcher
# Usage (from project root):
#   .\tools\test_client.ps1 host    # host client (gameDir=run/, built-in name Dev)
#   .\tools\test_client.ps1 slave   # 2nd client (gameDir=run-client2/, name Player2)
#   .\tools\test_client.ps1 dual    # launch host + slave BOTH concurrently (one command, LAN test)
#                                   # after both are up: host joins/hosts a world, pauses,
#                                   #   "Open to LAN" (allow cheats), then slave connects localhost:25565
#   .\tools\test_client.ps1 server  # headless test server
#   .\tools\test_client.ps1 clean   # only run pre-launch cleanup, do not start
#
# The script self-heals known startup traps before every launch:
#   * deletes stale run-client2/mods/laigu-dev.jar (handshake mismatch cause)
#   * by default KEEPS your world in run\saves (no auto-removal, so you don't rebuild a save each test)
#   * only with -IsolateSaves does it archive old worlds into saves_archive/ (for a real registry mismatch only)
# Options:
#   -StopDaemon     run ./gradlew --stop first (for a wedged daemon)
#   -NoClean        skip auto cleanup (defaults to clean)
#   -IsolateSaves   ALSO move old worlds out of saves/ into saves_archive/ (opt-in; use only on registry mismatch)
#   -Name <name>    override client username (default host=Dev / slave=Player2)
# Logs: appends to run-host.log / run-slave.log / run-server.log
# ============================================================================
param(
    [string]$Action = 'host',
    [switch]$StopDaemon,
    [switch]$NoClean,
    [switch]$IsolateSaves,
    [string]$Name
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot   # project root
Set-Location $root

# ---- 0. verify project root ----
if (-not (Test-Path "$root\gradlew.bat")) {
    Write-Host "[!] gradlew.bat not found; ensure cwd is the Laigu project root: $root" -ForegroundColor Red
    exit 1
}

# ---- 1. stop wedged daemon (only on demand) ----
if ($StopDaemon) {
    Write-Host "[*] stopping gradle daemon ..." -ForegroundColor Cyan
    & .\gradlew.bat --stop
}

# ---- 2. pre-launch cleanup (known traps) ----
if (-not $NoClean) {
    Write-Host "[*] pre-launch cleanup ..." -ForegroundColor Cyan

    # 2.1 stale dev jar on the slave dir (handshake mismatch root cause)
    $stale = "$root\run-client2\mods\laigu-dev.jar"
    if (Test-Path $stale) {
        Remove-Item $stale -Force
        Write-Host "    [x] removed stale dev jar: run-client2\mods\laigu-dev.jar" -ForegroundColor Yellow
    }

    # 2.2 isolate old worlds ONLY when explicitly requested (-IsolateSaves). By default we KEEP
    #     run\saves so the user does not have to rebuild their test world every launch.
    #     (Archiving is only for a genuine registry mismatch; the old auto-isolate kept wiping saves.)
    if ($IsolateSaves) {
        foreach ($dir in @('run', 'run-client2')) {
            $saves    = "$root\$dir\saves"
            $savesArc = "$root\$dir\saves_archive"
            if (Test-Path $saves) {
                Get-ChildItem $saves -Directory | ForEach-Object {
                    $dest = Join-Path $savesArc $_.Name
                    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
                    New-Item -ItemType Directory -Force -Path $savesArc | Out-Null
                    Move-Item $_.FullName -Destination $dest -Force
                    Write-Host "    [x] old world isolated: $($_.Name) -> $dir\saves_archive\" -ForegroundColor Yellow
                }
            }
        }
    } else {
        Write-Host "    [.] keeping world in run\saves (use -IsolateSaves to archive on registry mismatch)" -ForegroundColor DarkGray
    }
}

# ---- 3. build gradle command per action ----
switch ($Action.ToLower()) {
    'host' {
        if (-not $Name) { $Name = '' }
        $extra = if ($Name) { @("--username=$Name") } else { @() }
        $cmd   = @('runClient')
        $log   = "$root\run-host.log"
        $label = 'host client'
    }
    'slave' {
        if (-not $Name) { $Name = 'Player2' }
        $cmd   = @('runClient', '-PlaiguClientDir=run-client2', "--args=--username $Name")
        $extra = @()
        $log   = "$root\run-slave.log"
        $label = 'slave client'
    }
    'server' {
        $cmd   = @('runServer')
        $extra = @()
        $log   = "$root\run-server.log"
        $label = 'test server'
    }
    'clean' {
        Write-Host "[OK] pre-launch cleanup done; nothing started." -ForegroundColor Green
        exit 0
    }
    'dual' {
        # Launch host + slave concurrently as two detached processes.
        # Host name Dev (run/), slave name Player2 (run-client2/). Each logs separately.
        # After both reach the menu: host enters/opens a world -> pause -> "Open to LAN" (allow cheats),
        # then slave connects to localhost:25565.
        Write-Host "[*] launching host + slave clients concurrently ..." -ForegroundColor Green
        $cmdHost  = 'gradlew.bat runClient > "run-host.log" 2>&1'
        $cmdSlave = 'gradlew.bat runClient -PlaiguClientDir=run-client2 --args="--username Player2" > "run-slave.log" 2>&1'
        Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', $cmdHost)  -WorkingDirectory $root -WindowStyle Hidden
        Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', $cmdSlave) -WorkingDirectory $root -WindowStyle Hidden
        Write-Host "    progress -> run-host.log / run-slave.log" -ForegroundColor DarkGray
        Write-Host "    LAN flow: host opens a world -> pause -> Open to LAN (allow cheats); slave connects localhost:25565" -ForegroundColor DarkGray
        exit 0
    }
    default {
        Write-Host "[!] unknown action: $Action (available: host / slave / server / clean)" -ForegroundColor Red
        exit 1
    }
}

# ---- 4. launch ----
Write-Host "[*] launching $label ..." -ForegroundColor Green
Write-Host "    cmd : .\gradlew.bat $($cmd -join ' ') $($extra -join ' ')" -ForegroundColor DarkGray
Write-Host "    log : $log" -ForegroundColor DarkGray

# runClient/runServer is a long-running process; runs in foreground (call with a background job to poll the log)
& .\gradlew.bat @cmd @extra *> $log
exit $LASTEXITCODE
