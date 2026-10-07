<#
  Watchdog de dev-up.ps1 (solo desarrollo). Lanzado desacoplado por dev-up.ps1; no ejecutar a mano.
  Cada 10 s hace health-check del servidor (timeout 5 s); tras 2 fallos seguidos lo reinicia.
  Tambien relanza el simulador / queue si su proceso murio.
#>
param(
    [Parameter(Mandatory)][string]$Backend,
    [Parameter(Mandatory)][string]$Php,
    [int]$Port = 8000,
    [string]$OrgSlug = 'bucaratransit',
    [switch]$Simulate,
    [switch]$Queue
)
$ErrorActionPreference = 'Continue'
$Run = Join-Path $Backend 'storage\dev-up'
$Log = Join-Path $Backend 'storage\logs\dev-watchdog.log'
function Log($m) { Add-Content $Log ("{0} {1}" -f (Get-Date -Format 's'), $m) }

function Start-Detached($cmd) {
    # Win32_Process.Create: el proceso queda fuera del job/consola de quien lo lanza
    $r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = "cmd.exe /c $cmd"; CurrentDirectory = $Backend }
    if ($r.ReturnValue -ne 0) { Log "Create fallo rc=$($r.ReturnValue)"; return 0 }
    return [int]$r.ProcessId
}
function Alive($name) {
    $f = Join-Path $Run "$name.pid"
    if (-not (Test-Path $f)) { return $false }
    return [bool](Get-Process -Id ([int](Get-Content $f)) -ErrorAction SilentlyContinue)
}
function Kill-Tree($name) {
    $f = Join-Path $Run "$name.pid"
    if (Test-Path $f) { & taskkill /T /F /PID (Get-Content $f) 2>$null | Out-Null; Remove-Item $f -Force -ErrorAction SilentlyContinue }
}
function Start-Service($name) {
    switch ($name) {
        'serve'    { $cmd = "`"$Php`" artisan serve --host=0.0.0.0 --port=$Port >> storage\logs\dev-serve.out.log 2>&1" }
        'simulate' { $cmd = "`"$Php`" artisan demo:simulate-buses >> storage\logs\dev-simulate.out.log 2>&1" }
        'queue'    { $cmd = "`"$Php`" artisan queue:work --tries=1 >> storage\logs\dev-queue.out.log 2>&1" }
    }
    $id = Start-Detached $cmd
    if ($id) { Set-Content (Join-Path $Run "$name.pid") $id; Log "$name iniciado PID $id" }
}
function Healthy {
    try {
        $req = [System.Net.HttpWebRequest]::Create("http://127.0.0.1:$Port/api/v1/branding/$OrgSlug")
        $req.Timeout = 5000; $req.ReadWriteTimeout = 5000
        $resp = $req.GetResponse(); $ok = ($resp.StatusCode -eq 200); $resp.Close(); return $ok
    } catch { return $false }
}

$fails = 0
Log "watchdog iniciado (puerto $Port, org $OrgSlug)"
while ($true) {
    Start-Sleep -Seconds 10
    if (-not (Test-Path (Join-Path $Run 'watchdog.pid'))) { Log 'watchdog.pid desaparecio; salgo'; break }
    if (Healthy) { $fails = 0 } else { $fails++ }
    if ($fails -ge 2 -or -not (Alive 'serve')) {
        Log "servidor no responde (fallos=$fails); reiniciando"
        Kill-Tree 'serve'
        Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
            $p = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
            if ($p -and $p.ProcessName -match '^php') { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue }
        }
        Start-Sleep -Seconds 1
        Start-Service 'serve'
        $fails = 0
    }
    if ($Simulate -and -not (Alive 'simulate')) { Start-Service 'simulate' }
    if ($Queue -and -not (Alive 'queue')) { Start-Service 'queue' }
}
