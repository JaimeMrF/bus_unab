<#
.SYNOPSIS
  Levanta el backend Laravel en modo DEV/demo accesible desde la LAN (solo desarrollo).
.EXAMPLE
  .\scripts\dev-up.ps1                 # instala, prepara demo y arranca
  .\scripts\dev-up.ps1 -Fresh          # recrea la BD demo desde cero
  .\scripts\dev-up.ps1 -Simulate       # ademas simula buses en movimiento
  .\scripts\dev-up.ps1 -Emulator -CleanApp   # arranca AVD, compila e instala la app (10.0.2.2)
  .\scripts\dev-up.ps1 -Phone         # imprime el comando para un telefono fisico
  .\scripts\dev-up.ps1 -Stop           # detiene servidor, simulador y watchdog (no toca el emulador)
  .\scripts\dev-up.ps1 -Stop -Emulator # ademas cierra el emulador
#>
[CmdletBinding()]
param(
    [switch]$Fresh,
    [switch]$Stop,
    [switch]$Simulate,
    [switch]$Queue,
    [switch]$Emulator,
    [switch]$CleanApp,
    [switch]$Phone,
    [string]$Avd = '',
    [switch]$NoWatchdog,
    [int]$Port = 8000,
    [string]$OrgSlug = 'bucaratransit'
)
$ErrorActionPreference = 'Stop'
$Root    = Split-Path -Parent $PSScriptRoot
$Backend = Join-Path $Root 'bus_unab'
$PidFile = Join-Path $Backend 'storage\dev-up.pids'   # legado
$RunDir  = Join-Path $Backend 'storage\dev-up'
$AppId   = 'com.vibra.bus'

function Info($m) { Write-Host "[dev-up] $m" -ForegroundColor Cyan }
function Fail($m) { Write-Host "[dev-up] ERROR: $m" -ForegroundColor Red; exit 1 }

function Stop-Dev {
    # 1) watchdog primero (si no, reiniciaria el servidor); 2) servicios; 3) listener del puerto
    if (Test-Path $RunDir) {
        foreach ($n in 'watchdog', 'serve', 'simulate', 'queue') {
            $f = Join-Path $RunDir "$n.pid"
            if (Test-Path $f) {
                $id = (Get-Content $f | Select-Object -First 1)
                if ($id -match '^\d+$') {
                    if (Get-Process -Id ([int]$id) -ErrorAction SilentlyContinue) {
                        & taskkill /T /F /PID $id 2>$null | Out-Null
                        Info "detenido $n (arbol PID $id)"
                    }
                }
                Remove-Item $f -Force -ErrorAction SilentlyContinue
            }
        }
    }
    if (Test-Path $PidFile) {
        Get-Content $PidFile | ForEach-Object {
            if ($_ -match '^\d+$') {
                if (Get-Process -Id ([int]$_) -ErrorAction SilentlyContinue) {
                    & taskkill /T /F /PID $_ 2>$null | Out-Null
                }
            }
        }
        Remove-Item $PidFile -Force
    }
    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
        $p = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        if ($p -and $p.ProcessName -match '^php') { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; Info "detenido php en :$Port (PID $($p.Id))" }
    }
}

# -- Android SDK / JDK (solo para -Emulator) ----------------------------------
function Find-Sdk {
    foreach ($c in $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk')) {
        if ($c -and (Test-Path (Join-Path $c 'platform-tools\adb.exe'))) { return $c }
    }
    return $null
}
function Find-Jdk17 {
    $d = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($d) { return $d.FullName }
    if ($env:JAVA_HOME) { return $env:JAVA_HOME }
    return $null
}

if ($Stop) {
    Stop-Dev
    if ($Emulator) {
        $sdk = Find-Sdk
        if ($sdk) { & (Join-Path $sdk 'platform-tools\adb.exe') emu kill 2>$null | Out-Null; Info 'emulador cerrado (adb emu kill)' }
    }
    exit 0
}

# -- Requisitos ---------------------------------------------------------------
foreach ($c in 'php', 'composer') {
    if (-not (Get-Command $c -ErrorAction SilentlyContinue)) {
        Fail "'$c' no esta en PATH. Instala PHP 8.2+ y Composer (o usa docker compose, ver README)."
    }
}
$phpMods = (& php -m) -join ','
if ($phpMods -notmatch '(?im)(^|,)gd(,|$)') { Write-Warning "extension PHP 'gd' no habilitada (opcional; actÃ­vala en php.ini si subes imÃ¡genes)." }
foreach ($m in 'mbstring', 'pdo_sqlite', 'intl', 'bcmath') {
    if ($phpMods -notmatch "(?im)(^|,)$m(,|$)") { Fail "extension PHP '$m' no habilitada (php.ini)." }
}

# -- IP LAN -------------------------------------------------------------------
$Ip = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -match '^(192\.168|10\.|172\.(1[6-9]|2\d|3[01]))' -and $_.InterfaceAlias -notmatch 'vEthernet|WSL|Loopback|VirtualBox|VMware' } |
    Sort-Object InterfaceMetric | Select-Object -First 1).IPAddress
if (-not $Ip) { $Ip = '127.0.0.1'; Write-Warning 'No se detecto IP LAN; usando 127.0.0.1 (solo emulador via 10.0.2.2).' }
$AppUrl = "http://${Ip}:$Port"

function Set-Env($key, $value) {
    $c = Get-Content .env -Raw
    if ($c -match "(?m)^#?\s*$key=") { $c = [regex]::Replace($c, "(?m)^#?\s*$key=.*$", "$key=$value") }
    else { $c = $c.TrimEnd() + "`n$key=$value`n" }
    Set-Content .env $c -NoNewline -Encoding utf8
}

Push-Location $Backend
try {
    # -- Dependencias ---------------------------------------------------------
    if (-not (Test-Path 'vendor\autoload.php')) {
        Info 'composer install'
        composer install --no-interaction --prefer-dist
        if ($LASTEXITCODE) { Fail 'composer install fallo' }
    }

    # -- .env (se crea si falta; APP_URL y CORS se actualizan siempre) --------
    $created = $false
    if (-not (Test-Path '.env')) { Copy-Item '.env.example' '.env'; $created = $true; Info '.env creado desde .env.example' }
    Set-Env 'APP_URL' $AppUrl
    Set-Env 'CORS_ALLOWED_ORIGINS' "$AppUrl,http://localhost:$Port,http://10.0.2.2:$Port"
    if ($created) { Set-Env 'APP_DEBUG' 'true' }
    if (-not (Select-String -Path .env -Pattern '^APP_KEY=base64:' -Quiet)) { php artisan key:generate --force | Out-Null }
    if ((Select-String -Path .env -Pattern '^DB_CONNECTION=sqlite' -Quiet) -and -not (Test-Path 'database\database.sqlite')) {
        New-Item -ItemType File 'database\database.sqlite' | Out-Null
    }

    # -- Detener instancias previas, preparar demo ----------------------------
    Stop-Dev
    if ($Fresh) { Info 'demo:setup (BD nueva, destructivo)'; php artisan demo:setup }
    else { Info 'migrate + demo:setup --no-fresh'; php artisan migrate --force; if ($LASTEXITCODE) { Fail 'migrate fallo' }; php artisan demo:setup --no-fresh }
    if ($LASTEXITCODE) { Fail 'demo:setup fallo' }
    php artisan storage:link 2>$null | Out-Null

    # -- Arranque -------------------------------------------------------------
    New-Item -ItemType Directory -Force 'storage\logs' | Out-Null
    New-Item -ItemType Directory -Force $RunDir | Out-Null
    $php = (Get-Command php).Source
    function Start-Detached($name, $cmd) {
        # Win32_Process.Create: el proceso queda fuera del job/consola de esta sesion y sobrevive a su cierre
        $r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = "cmd.exe /c $cmd"; CurrentDirectory = (Get-Location).Path }
        if ($r.ReturnValue -ne 0) { Fail "no se pudo lanzar $name (rc=$($r.ReturnValue))" }
        Set-Content (Join-Path $RunDir "$name.pid") $r.ProcessId
    }
    Start-Detached 'serve' "`"$php`" artisan serve --host=0.0.0.0 --port=$Port >> storage\logs\dev-serve.out.log 2>&1"
    if ($Queue)    { Start-Detached 'queue'    "`"$php`" artisan queue:work --tries=1 >> storage\logs\dev-queue.out.log 2>&1" }
    if ($Simulate) { Start-Detached 'simulate' "`"$php`" artisan demo:simulate-buses >> storage\logs\dev-simulate.out.log 2>&1" }
    if (-not $NoWatchdog) {
        $wd = Join-Path $PSScriptRoot 'dev-watchdog.ps1'
        $wdArgs = "-Backend `"$Backend`" -Php `"$php`" -Port $Port -OrgSlug $OrgSlug"
        if ($Simulate) { $wdArgs += ' -Simulate' }
        if ($Queue)    { $wdArgs += ' -Queue' }
        Start-Detached 'watchdog' "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$wd`" $wdArgs"
    }

    # -- Firewall -------------------------------------------------------------
    $rule = "BusDev-$Port"
    $fwCmd = "New-NetFirewallRule -DisplayName '$rule' -Direction Inbound -Protocol TCP -LocalPort $Port -Action Allow -Profile Private"
    $isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    if (-not (Get-NetFirewallRule -DisplayName $rule -ErrorAction SilentlyContinue)) {
        if ($isAdmin) { Invoke-Expression $fwCmd | Out-Null; Info "regla de firewall '$rule' creada (perfil Privado)" }
        else { Write-Warning "Sin admin: para que el telefono conecte, ejecuta en PowerShell como administrador:`n  $fwCmd" }
    }

    # -- Health check ---------------------------------------------------------
    $ok = $false
    for ($i = 0; $i -lt 30 -and -not $ok; $i++) {
        try { $r = Invoke-WebRequest "http://127.0.0.1:$Port/api/v1/branding/$OrgSlug" -UseBasicParsing -TimeoutSec 3; $ok = ($r.StatusCode -eq 200) }
        catch { Start-Sleep -Seconds 1 }
    }
    if (-not $ok) { Fail "el servidor no respondio en /api/v1/branding/$OrgSlug. Revisa storage\logs\dev-serve.err.log" }
}
finally { Pop-Location }

# -- Emulador Android ---------------------------------------------------------
$gradleCmd = "./gradlew :composeApp:installDebug -PapiBaseUrl=http://10.0.2.2:$Port/api/v1 -PdefaultOrgSlug=$OrgSlug"
$emuResult = $null
if ($Emulator) {
    $sdk = Find-Sdk
    if (-not $sdk) { Fail 'Android SDK no encontrado (define ANDROID_HOME o instala en %LOCALAPPDATA%\Android\Sdk).' }
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    $emu = Join-Path $sdk 'emulator\emulator.exe'
    $jdk = Find-Jdk17
    if ($jdk) { $env:JAVA_HOME = $jdk; Info "JAVA_HOME=$jdk" } else { Write-Warning 'JDK 17 no encontrado; usando el del PATH.' }

    $running = (& $adb devices) -match '^emulator-\d+\s+device'
    if (-not $running) {
        if (-not $Avd) { $Avd = (& $emu -list-avds | Where-Object { $_ -match '\S' } | Select-Object -First 1) }
        if (-not $Avd) { Fail 'No hay AVDs (emulator -list-avds vacio). Crea uno en Android Studio.' }
        Info "lanzando AVD '$Avd' (desacoplado)"
        $r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = "`"$emu`" -avd $Avd -no-snapshot-save -no-boot-anim"; CurrentDirectory = $sdk }
        if ($r.ReturnValue -ne 0) { Fail "no se pudo lanzar el emulador (rc=$($r.ReturnValue))" }
    } else { Info 'emulador ya en ejecucion' }

    & $adb wait-for-device
    Info 'esperando sys.boot_completed...'
    $booted = $false
    for ($i = 0; $i -lt 120 -and -not $booted; $i++) {
        $v = (& $adb shell getprop sys.boot_completed 2>$null)
        if ("$v".Trim() -eq '1') { $booted = $true } else { Start-Sleep -Seconds 3 }
    }
    if (-not $booted) { Fail 'el emulador no termino de arrancar (6 min)' }

    Info 'compilando e instalando la app (la primera vez puede tardar varios minutos)...'
    Push-Location (Join-Path $Root 'frontend')
    try {
        & .\gradlew.bat ':composeApp:installDebug' "-PapiBaseUrl=http://10.0.2.2:$Port/api/v1" "-PdefaultOrgSlug=$OrgSlug"
        if ($LASTEXITCODE) { Fail 'gradle installDebug fallo' }
    } finally { Pop-Location }

    if ($CleanApp) { Info 'pm clear'; & $adb shell pm clear $AppId | Out-Null }
    foreach ($perm in 'ACCESS_FINE_LOCATION', 'ACCESS_COARSE_LOCATION', 'CAMERA', 'POST_NOTIFICATIONS') {
        & $adb shell pm grant $AppId "android.permission.$perm" 2>$null | Out-Null
    }
    & $adb shell am start -n "$AppId/$AppId.MainActivity" | Out-Null
    Start-Sleep -Seconds 8
    $pidApp = "$(& $adb shell pidof $AppId 2>$null)".Trim()
    $focus = (& $adb shell dumpsys window 2>$null | Select-String 'mCurrentFocus' | Select-Object -First 1).Line
    $shot = Join-Path $Backend 'storage\logs\dev-emulator.png'
    cmd /c "`"$adb`" exec-out screencap -p > `"$shot`""
    $emuResult = if ($pidApp -and $focus -match [regex]::Escape($AppId)) { "app abierta (PID $pidApp), captura: $shot" } else { "LA APP NO PARECE ABIERTA (pid='$pidApp', focus='$focus')" }
    Info $emuResult
}


$gradle = "./gradlew :composeApp:installDebug -PapiBaseUrl=$AppUrl/api/v1 -PdefaultOrgSlug=$OrgSlug"
Write-Host ''
Write-Host '================ BUS DEV LISTO (solo desarrollo) ================' -ForegroundColor Green
Write-Host " Servidor : $AppUrl   (health OK)"
Write-Host " API      : $AppUrl/api/v1"
Write-Host " Panel    : $AppUrl/admin  |  $AppUrl/empresa"
Write-Host " Orgs demo: bucaratransit, metrobus, campus, logistica  (default: $OrgSlug)"
Write-Host ' Login    : admin.<slug>@demo.test / driver.<slug>@demo.test / pasajero.<slug>@demo.test  -  clave Demo12345!'
Write-Host ' Super    : superadmin@demo.test (panel /admin; tenant en /empresa)  -  detalle: docs/DEMO.md'
if ($Simulate) { Write-Host ' Simulador: activo (demo:simulate-buses)' }
if (-not $NoWatchdog) { Write-Host ' Watchdog : activo (health-check 10 s, reinicia el servidor si no responde)' }
if ($emuResult) { Write-Host " Emulador : $emuResult" }
Write-Host ' Detener  : .\scripts\dev-up.ps1 -Stop   (con -Emulator tambien cierra el AVD)'
Write-Host ''
Write-Host ' Build para TELEFONO FISICO (misma red Wi-Fi; desde frontend/):' -ForegroundColor Yellow
Write-Host "   $gradle"
if ($Phone) { Write-Host " TELEFONO: conecta al mismo Wi-Fi y ejecuta el comando de arriba (IP LAN $Ip). Si no conecta, abre el firewall (ver aviso previo)." -ForegroundColor Green }
Write-Host ' Build para EMULADOR (o usa -Emulator):' -ForegroundColor Yellow
Write-Host "   $gradleCmd"
Write-Host '================================================================='

