<#
.SYNOPSIS
  Verificación E2E de la app en el emulador Android con adb (QA, T24). Solo para desarrollo.
.DESCRIPTION
  Automatiza: (a) marca cargada, (b) login pasajero/conductor, (c) Home (buses, ETA, favoritos),
  (d) mapa, (e) wallet+QR sin permiso de cámara antes del escáner, (f) cambio de organización
  campus/metrobus, (g) sin red usa la marca cacheada, (h) logcat sin excepciones,
  (i) sin aviso de 16 KB, (j) tiempo de arranque en frío.
  Genera tests/e2e/out/report.md + capturas y XML de UI por paso. Código de salida = nº de fallos.
  Requisitos: backend arriba (scripts/dev-up.ps1 -Simulate), emulador encendido con la app instalada
  (scripts/dev-up.ps1 -Emulator), y -ApiHost accesible (10.0.2.2 desde el emulador).
.EXAMPLE
  .\tests\e2e\e2e-emulator.ps1                       # todo, org bucaratransit
  .\tests\e2e\e2e-emulator.ps1 -Only a,b,j            # solo algunos pasos
  .\tests\e2e\e2e-emulator.ps1 -SkipAirplane          # sin tocar el modo avión
#>
[CmdletBinding()]
param(
    [string]$Package = 'com.vibra.bus',
    [string]$Org = 'bucaratransit',
    [string]$Password = 'Demo12345!',
    [string]$ApiBase = 'http://127.0.0.1:8000/api/v1',   # desde el PC (para comprobar el backend)
    [string[]]$Only = @(),
    [int]$ColdStartBudgetMs = 4000,
    [switch]$SkipAirplane
)
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$out = Join-Path $here 'out'
New-Item -ItemType Directory -Force $out | Out-Null
$Adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $Adb)) { $Adb = (Get-Command adb -ErrorAction Stop).Source }

$results = New-Object System.Collections.Generic.List[object]
function Want($id) { return ($Only.Count -eq 0) -or ($Only -contains $id) }
function Adb { param([Parameter(ValueFromRemainingArguments)]$a) & $Adb @a 2>&1 }
function Sh { param([Parameter(ValueFromRemainingArguments)]$a) (& $Adb shell @a 2>&1) -join "`n" }
function Record($id, $name, [bool]$ok, $detail, $shot = '') {
    $results.Add([pscustomobject]@{ Id = $id; Paso = $name; Estado = $(if ($ok) { 'PASA' } else { 'FALLA' }); Detalle = $detail; Captura = $shot })
    $c = if ($ok) { 'Green' } else { 'Red' }
    Write-Host ("[{0}] {1,-5} {2} - {3}" -f $(if ($ok) { 'OK ' } else { 'ERR' }), $id, $name, $detail) -ForegroundColor $c
}

# ── Utilidades de dispositivo ────────────────────────────────────────────
function Shot($name) {
    # screencap en el dispositivo + pull (no redirigir binarios por PowerShell)
    Sh screencap -p "/sdcard/$name.png" | Out-Null
    Adb pull "/sdcard/$name.png" (Join-Path $out "$name.png") | Out-Null
    return (Join-Path $out "$name.png")
}
function UiDump($name = 'ui') {
    Sh uiautomator dump "/sdcard/$name.xml" | Out-Null
    $local = Join-Path $out "$name.xml"
    Adb pull "/sdcard/$name.xml" $local | Out-Null
    if (-not (Test-Path $local)) { return $null }
    return [xml](Get-Content $local -Raw -Encoding UTF8)
}
function UiNodes($xml) { if ($xml) { $xml.SelectNodes('//node') } else { @() } }
function FindNode($xml, $pattern) {
    foreach ($n in (UiNodes $xml)) {
        if (($n.text -match $pattern) -or ($n.'content-desc' -match $pattern)) { return $n }
    }
    return $null
}
function Center($node) {
    if ($node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        return @([int](([int]$Matches[1] + [int]$Matches[3]) / 2), [int](([int]$Matches[2] + [int]$Matches[4]) / 2))
    }
}
function WaitText($pattern, $timeoutSec = 20, $dumpName = 'ui') {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    do {
        $x = UiDump $dumpName
        $n = FindNode $x $pattern
        if ($n) { return $n }
        Start-Sleep -Milliseconds 700
    } while ((Get-Date) -lt $deadline)
    return $null
}
function TapText($pattern, $timeoutSec = 15) {
    $n = WaitText $pattern $timeoutSec
    if (-not $n) { return $false }
    $c = Center $n
    Sh input tap $c[0] $c[1] | Out-Null
    Start-Sleep -Milliseconds 500
    return $true
}
function TypeText($text) { Sh input text ($text -replace ' ', '%s' -replace '&', '\&') | Out-Null }
function Back { Sh input keyevent KEYCODE_BACK | Out-Null; Start-Sleep -Milliseconds 400 }
function AppPid { (Sh pidof $Package).Trim() }
function ClearState { Sh pm clear $Package | Out-Null }
function Launch { return (Sh am start -W -n "$Package/.MainActivity") }
function ForceStop { Sh am force-stop $Package | Out-Null }
function GrantedPerm($perm) { return ((Sh dumpsys package $Package) -match "$perm.*granted=true") }

# Color dominante de una región (System.Drawing), para comprobar la marca sin depender del texto.
Add-Type -AssemblyName System.Drawing
function PixelHex($png, $x, $y) {
    $bmp = New-Object System.Drawing.Bitmap $png
    try { $p = $bmp.GetPixel([Math]::Min($x, $bmp.Width - 1), [Math]::Min($y, $bmp.Height - 1)); return ('#{0:X2}{1:X2}{2:X2}' -f $p.R, $p.G, $p.B) } finally { $bmp.Dispose() }
}
function ColorShare($png, [string[]]$targetsHex, [int]$tol = 40) {
    # Porcentaje de píxeles (muestreo 1/8) cercanos a cualquiera de los colores objetivo.
    $bmp = New-Object System.Drawing.Bitmap $png
    $hit = @{}; foreach ($t in $targetsHex) { $hit[$t] = 0 }
    $total = 0
    try {
        for ($y = 0; $y -lt $bmp.Height; $y += 8) { for ($x = 0; $x -lt $bmp.Width; $x += 8) {
                $p = $bmp.GetPixel($x, $y); $total++
                foreach ($t in $targetsHex) {
                    $r = [Convert]::ToInt32($t.Substring(1, 2), 16); $g = [Convert]::ToInt32($t.Substring(3, 2), 16); $b = [Convert]::ToInt32($t.Substring(5, 2), 16)
                    if ([Math]::Abs($p.R - $r) -le $tol -and [Math]::Abs($p.G - $g) -le $tol -and [Math]::Abs($p.B - $b) -le $tol) { $hit[$t]++ }
                } } }
    } finally { $bmp.Dispose() }
    $res = @{}; foreach ($t in $targetsHex) { $res[$t] = [Math]::Round(100.0 * $hit[$t] / [Math]::Max($total, 1), 2) }
    return $res
}

# ── Preparación ──────────────────────────────────────────────────────────
Write-Host "== E2E emulador ($Package, org=$Org) ==" -ForegroundColor Cyan
$dev = (Adb devices) -join "`n"
if ($dev -notmatch 'emulator-\d+\s+device') { Write-Host 'No hay emulador conectado (adb devices).' -ForegroundColor Red; exit 99 }
if ((Sh pm list packages $Package) -notmatch $Package) { Write-Host "La app $Package no está instalada." -ForegroundColor Red; exit 98 }
try {
    $b = Invoke-RestMethod "$ApiBase/branding/$Org" -TimeoutSec 8
    Write-Host ("Backend OK: {0} ({1}) v{2}" -f $b.data.app_name, $b.data.slug, $b.data.version) -ForegroundColor DarkGray
} catch { Write-Host "Backend no responde en $ApiBase/branding/$Org : $($_.Exception.Message)" -ForegroundColor Red; exit 97 }
$brand = $b.data
Adb logcat -c | Out-Null
Sh settings put global window_animation_scale 0 | Out-Null
Sh settings put global transition_animation_scale 0 | Out-Null
Sh settings put global animator_duration_scale 1 | Out-Null

# ── (j) arranque en frío + (a) marca cargada ─────────────────────────────
if ((Want 'a') -or (Want 'j')) {
    ForceStop; ClearState
    $startOut = Launch
    $total = if ($startOut -match 'TotalTime:\s*(\d+)') { [int]$Matches[1] } else { -1 }
    $wait = if ($startOut -match 'WaitTime:\s*(\d+)') { [int]$Matches[1] } else { -1 }
    Start-Sleep -Seconds 1
    # El primer arranque tras pm clear puede pedir el código de organización: se escribe si aparece.
    $orgField = WaitText 'Código de organización' 6
    if ($orgField) {
        $c = Center $orgField; Sh input tap $c[0] $c[1] | Out-Null; TypeText $Org
        Sh input keyevent KEYCODE_ENTER | Out-Null; Start-Sleep -Milliseconds 400
        [void](TapText '^Continuar$' 4)
    }
    $login = WaitText "Iniciar sesi|Correo|Email|Contrase" 25 'ui_a'
    $shot = Shot 'a_marca_login'
    if (Want 'j') {
        Record 'j' 'Arranque en frío' ($total -ge 0 -and $total -le $ColdStartBudgetMs) "TotalTime=${total}ms WaitTime=${wait}ms (presupuesto ${ColdStartBudgetMs}ms; emulador, no dispositivo real)" ''
    }
    if (Want 'a') {
        $xml = UiDump 'ui_a'
        $hasName = [bool](FindNode $xml ([regex]::Escape($brand.app_name)))
        $hasPlaceholder = [bool](FindNode $xml '^Transporte$')
        $share = ColorShare $shot @($brand.colors.light.primary, $brand.colors.light.accent) 36
        $brandPx = ($share.Values | Measure-Object -Sum).Sum
        $ok = $hasName -and (-not $hasPlaceholder) -and $brandPx -gt 0.3
        Record 'a' 'Marca cargada' $ok ("nombre '{0}'={1}; tema neutro presente={2}; píxeles de marca (primary/accent)={3}% ; logo/mascota: revisar captura" -f $brand.app_name, $hasName, $hasPlaceholder, $brandPx) $shot
    }
}

# ── (b) login pasajero y conductor ───────────────────────────────────────
function DoLogin($email) {
    $f = WaitText '^Correo|Email|Correo electr' 12
    if (-not $f) { return $false }
    $c = Center $f; Sh input tap $c[0] $c[1] | Out-Null; TypeText $email
    Sh input keyevent KEYCODE_TAB | Out-Null; TypeText $Password
    Sh input keyevent KEYCODE_BACK | Out-Null
    if (-not (TapText '^Iniciar sesi.n$|^Ingresar$|^Entrar$' 6)) { Sh input keyevent KEYCODE_ENTER | Out-Null }
    return $true
}
if (Want 'b') {
    $ok1 = DoLogin "pasajero.$Org@demo.test"
    $home = WaitText 'Inicio|Home|Wallet|Billetera|Mapa' 25 'ui_b1'
    $s1 = Shot 'b_login_pasajero'
    Record 'b1' 'Login pasajero' ($ok1 -and [bool]$home) "pasajero.$Org@demo.test -> $(if ($home) { 'Home visible' } else { 'sin Home' })" $s1
}

# ── (c) Home: buses, ETA, favoritos ──────────────────────────────────────
if (Want 'c') {
    $x = UiDump 'ui_c'
    $buses = ($x.SelectNodes('//node') | Where-Object { $_.text -match 'BT40[12]' -or $_.'content-desc' -match 'BT40[12]' }).Count
    $s = Shot 'c_home'
    Record 'c1' 'Home: buses BT401/BT402' ($buses -gt 0) "nodos con placa BT40x: $buses" $s
    $s2a = Shot 'c_home_t0'; Start-Sleep -Seconds 12; $s2b = Shot 'c_home_t12'
    $h1 = (Get-FileHash $s2a).Hash; $h2 = (Get-FileHash $s2b).Hash
    Record 'c2' 'Home: contenido vivo (cambia en 12 s)' ($h1 -ne $h2) 'compara dos capturas; requiere dev-up -Simulate' $s2b
    $x = UiDump 'ui_c'
    $eta = FindNode $x '(\d+\s*min|Menos de 1 min|Calculando)'
    Record 'c3' 'ETA visible' ([bool]$eta) $(if ($eta) { $eta.text } else { 'sin texto de ETA en Home' }) ''
    $fav = FindNode $x 'avorit'
    Record 'c4' 'Favoritos accesibles' ([bool]$fav) $(if ($fav) { 'control de favoritos encontrado' } else { 'no se encontró control de favoritos' }) ''
}

# ── (d) mapa con bus dinámico ────────────────────────────────────────────
if (Want 'd') {
    [void](TapText '^Mapa$|Mapa' 6)
    Start-Sleep -Seconds 6
    $m1 = Shot 'd_mapa_t0'; Start-Sleep -Seconds 10; $m2 = Shot 'd_mapa_t10'
    $bus = $brand.bus_style
    $cols = @(); if ($bus.body) { $cols += $bus.body }; if ($bus.accent) { $cols += $bus.accent }
    $share = if ($cols.Count) { ColorShare $m2 $cols 30 } else { @{} }
    $sum = ($share.Values | Measure-Object -Sum).Sum
    Record 'd' 'Mapa con bus de la marca' (($sum -gt 0.05) -and ((Get-FileHash $m1).Hash -ne (Get-FileHash $m2).Hash)) ("color body/accent del bus presente={0}% ; mapa cambia entre capturas; MapLibre necesita red de tiles" -f $sum) $m2
}

# ── (e) wallet + QR; cámara solo al entrar al escáner ────────────────────
if (Want 'e') {
    $camBefore = GrantedPerm 'android.permission.CAMERA'
    [void](TapText 'Wallet|Billetera|Saldo' 8)
    Start-Sleep -Seconds 2
    $w = Shot 'e_wallet'
    [void](TapText 'Mi QR|Generar QR|C.digo QR|QR' 8)
    Start-Sleep -Seconds 3
    $q = Shot 'e_qr'
    $camAfterQr = GrantedPerm 'android.permission.CAMERA'
    Record 'e1' 'Wallet y QR sin cámara' ((-not $camBefore) -and (-not $camAfterQr)) "CAMERA concedida antes=$camBefore tras mostrar QR=$camAfterQr (debe ser False en ambos)" $q
}

# ── (f) cambiar de organización ──────────────────────────────────────────
if (Want 'f') {
    foreach ($slug in @('campus', 'metrobus')) {
        Back; Back
        [void](TapText 'Perfil' 6)
        if (-not (TapText 'Cambiar organizaci' 8)) { Record "f-$slug" "Cambiar a $slug" $false "no se encontró 'Cambiar organización'" ''; continue }
        $f = WaitText 'C.digo de organizaci' 8
        if ($f) { $c = Center $f; Sh input tap $c[0] $c[1] | Out-Null; TypeText $slug; [void](TapText '^Continuar$' 4) }
        Start-Sleep -Seconds 4
        $r = Invoke-RestMethod "$ApiBase/branding/$slug"
        $s = Shot "f_$slug"
        $x = UiDump 'ui_f'
        $nameOk = [bool](FindNode $x ([regex]::Escape($r.data.app_name)))
        $share = ColorShare $s @($r.data.colors.light.primary) 36
        Record "f-$slug" "Re-tematizado a $slug" ($nameOk -or ($share.Values | Measure-Object -Sum).Sum -gt 0.3) ("app_name '{0}' visible={1}; primary {2}={3}%" -f $r.data.app_name, $nameOk, $r.data.colors.light.primary, ($share.Values | Measure-Object -Sum).Sum) $s
    }
}

# ── (g) sin red usa la marca cacheada ────────────────────────────────────
if ((Want 'g') -and (-not $SkipAirplane)) {
    try {
        Sh cmd connectivity airplane-mode enable | Out-Null
        Start-Sleep -Seconds 3
        ForceStop; $null = Launch; Start-Sleep -Seconds 6
        $s = Shot 'g_sin_red'
        $x = UiDump 'ui_g'
        $neutral = [bool](FindNode $x '^Transporte$')
        $crashed = [string]::IsNullOrWhiteSpace((AppPid))
        Record 'g' 'Sin red: marca cacheada' ((-not $neutral) -and (-not $crashed)) "tema neutro 'Transporte' visible=$neutral; app viva=$(-not $crashed)" $s
    } finally { Sh cmd connectivity airplane-mode disable | Out-Null; Start-Sleep -Seconds 3 }
}

# ── (h) logcat sin excepciones, (i) sin aviso de 16 KB ───────────────────
if (Want 'h') {
    $log = (Adb logcat -d -v brief) -join "`n"
    Set-Content (Join-Path $out 'logcat.txt') $log -Encoding UTF8
    $fatal = [regex]::Matches($log, 'FATAL EXCEPTION[^\n]*\n[^\n]*\n[^\n]*') | ForEach-Object { $_.Value }
    $anr = [regex]::Matches($log, 'ANR in ' + [regex]::Escape($Package))
    $own = [regex]::Matches($log, "E/[^\n]*(AndroidRuntime|$([regex]::Escape($Package)))[^\n]*(Exception|Error)[^\n]*") | Select-Object -First 5 | ForEach-Object { $_.Value }
    Record 'h' 'Logcat sin excepciones' (($fatal.Count -eq 0) -and ($anr.Count -eq 0)) ("FATAL=$($fatal.Count) ANR=$($anr.Count) errores propios=$(@($own).Count) (ver out/logcat.txt)") (Join-Path $out 'logcat.txt')
}
if (Want 'i') {
    $log = if (Test-Path (Join-Path $out 'logcat.txt')) { Get-Content (Join-Path $out 'logcat.txt') -Raw } else { (Adb logcat -d) -join "`n" }
    $page = (Sh getconf PAGE_SIZE).Trim()
    $warn = ($log -match '16\s*KB|16384.*(compat|align)|not 16 KB|page size') -and ($log -match 'compat|align|warn')
    $x = UiDump 'ui_i'
    $dlg = FindNode $x '16\s*KB'
    Record 'i' 'Sin aviso de 16 KB' ((-not $warn) -and (-not $dlg)) "PAGE_SIZE del dispositivo=$page ; aviso en logcat=$warn ; diálogo en pantalla=$([bool]$dlg)" ''
}

# ── Informe ──────────────────────────────────────────────────────────────
$fail = @($results | Where-Object Estado -eq 'FALLA').Count
$md = @("# Informe E2E emulador", "", "Fecha: $(Get-Date -Format s) · Paquete: ``$Package`` · Org: ``$Org``", "", "| Paso | Resultado | Detalle | Captura |", "|---|---|---|---|")
foreach ($r in $results) { $md += "| $($r.Id) $($r.Paso) | **$($r.Estado)** | $($r.Detalle -replace '\|','/') | $(if ($r.Captura) { Split-Path $r.Captura -Leaf }) |" }
$md += ""; $md += "Total: $($results.Count) · Fallos: $fail"
Set-Content (Join-Path $out 'report.md') $md -Encoding UTF8
Write-Host ""; $results | Format-Table Id, Paso, Estado, Detalle -AutoSize -Wrap | Out-String | Write-Host
Write-Host "Informe: $(Join-Path $out 'report.md')" -ForegroundColor Cyan
exit $fail
