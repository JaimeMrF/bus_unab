<#
.SYNOPSIS
  Levanta el backend Laravel en modo DEV/demo accesible desde la LAN (solo desarrollo).
.EXAMPLE
  .\scripts\dev-up.ps1                 # instala, prepara demo y arranca
  .\scripts\dev-up.ps1 -Fresh          # recrea la BD demo desde cero
  .\scripts\dev-up.ps1 -Simulate       # ademas simula buses en movimiento
  .\scripts\dev-up.ps1 -Stop           # detiene lo que lanzo este script
#>
[CmdletBinding()]
param(
    [switch]$Fresh,
    [switch]$Stop,
    [switch]$Simulate,
    [switch]$Queue,
    [int]$Port = 8000,
    [string]$OrgSlug = 'metrobus'
)
$ErrorActionPreference = 'Stop'
$Root    = Split-Path -Parent $PSScriptRoot
$Backend = Join-Path $Root 'bus_unab'
$PidFile = Join-Path $Backend 'storage\dev-up.pids'

function Info($m) { Write-Host "[dev-up] $m" -ForegroundColor Cyan }
function Fail($m) { Write-Host "[dev-up] ERROR: $m" -ForegroundColor Red; exit 1 }

function Stop-Dev {
    if (Test-Path $PidFile) {
        Get-Content $PidFile | ForEach-Object {
            $p = Get-Process -Id ([int]$_) -ErrorAction SilentlyContinue
            if ($p) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; Info "detenido PID $($p.Id)" }
        }
        Remove-Item $PidFile -Force
    } else { Info 'nada que detener' }
}

if ($Stop) { Stop-Dev; exit 0 }

# -- Requisitos ---------------------------------------------------------------
foreach ($c in 'php', 'composer') {
    if (-not (Get-Command $c -ErrorAction SilentlyContinue)) {
        Fail "'$c' no esta en PATH. Instala PHP 8.3+ y Composer (o usa docker compose, ver README)."
    }
}
$phpMods = (& php -m) -join ','
foreach ($m in 'gd', 'mbstring', 'pdo_sqlite', 'intl', 'bcmath') {
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
    $pids = @()
    $srv = Start-Process php -ArgumentList 'artisan', 'serve', '--host=0.0.0.0', "--port=$Port" -PassThru -WindowStyle Hidden `
        -RedirectStandardOutput 'storage\logs\dev-serve.out.log' -RedirectStandardError 'storage\logs\dev-serve.err.log'
    $pids += $srv.Id
    if ($Queue) {
        $q = Start-Process php -ArgumentList 'artisan', 'queue:work', '--tries=1' -PassThru -WindowStyle Hidden
        $pids += $q.Id
    }
    if ($Simulate) {
        $s = Start-Process php -ArgumentList 'artisan', 'demo:simulate-buses' -PassThru -WindowStyle Hidden `
            -RedirectStandardOutput 'storage\logs\dev-simulate.out.log' -RedirectStandardError 'storage\logs\dev-simulate.err.log'
        $pids += $s.Id
    }
    $pids | Set-Content $PidFile

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

$gradle = "./gradlew :composeApp:installDebug -PapiBaseUrl=$AppUrl/api/v1 -PdefaultOrgSlug=$OrgSlug"
Write-Host ''
Write-Host '================ BUS DEV LISTO (solo desarrollo) ================' -ForegroundColor Green
Write-Host " Servidor : $AppUrl   (health OK)"
Write-Host " API      : $AppUrl/api/v1"
Write-Host " Panel    : $AppUrl/admin  |  $AppUrl/empresa"
Write-Host " Orgs demo: metrobus, campus, logistica  (default: $OrgSlug)"
Write-Host ' Login    : admin.<slug>@demo.test / driver.<slug>@demo.test / pasajero.<slug>@demo.test  -  clave Demo12345!'
Write-Host ' Super    : superadmin@demo.test (panel /admin; tenant en /empresa)  -  detalle: docs/DEMO.md'
if ($Simulate) { Write-Host ' Simulador: activo (demo:simulate-buses)' }
Write-Host ' Detener  : .\scripts\dev-up.ps1 -Stop'
Write-Host ''
Write-Host ' Build de la app (desde frontend/):' -ForegroundColor Yellow
Write-Host "   $gradle"
Write-Host '================================================================='
