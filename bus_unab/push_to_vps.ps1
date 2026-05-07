# push_to_vps.ps1 — Run from Windows to deploy the backend to the VPS
# Usage:  .\push_to_vps.ps1 -User root
# Prereqs: OpenSSH client (comes with Windows 10/11) and git

param(
    [string]$User = "root",
    [string]$VpsHost = "79.143.89.188",
    [string]$AppDir = "/opt/bus_unab/bus_unab"
)

$Target = "${User}@${VpsHost}"

Write-Host "=== Bus UNAB — Push to VPS ===" -ForegroundColor Green

# ── 1. Push latest commits to GitHub first ────────────────────────────────────
Write-Host "[1/5] Pushing to GitHub..." -ForegroundColor Cyan
git push
if ($LASTEXITCODE -ne 0) { Write-Host "git push failed" -ForegroundColor Red; exit 1 }

# ── 2. Ensure app dir exists on VPS and copy required files ───────────────────
Write-Host "[2/5] Ensuring app dir and copying files to VPS..." -ForegroundColor Cyan
ssh $Target "mkdir -p $AppDir"
if ($LASTEXITCODE -ne 0) {
    Write-Host "SSH failed. Make sure SSH access works: ssh ${Target}" -ForegroundColor Red
    exit 1
}

scp .env.production "${Target}:${AppDir}/.env.production"
if ($LASTEXITCODE -ne 0) { Write-Host "SCP .env.production failed" -ForegroundColor Red; exit 1 }

scp deploy.sh "${Target}:${AppDir}/deploy.sh"
if ($LASTEXITCODE -ne 0) { Write-Host "SCP deploy.sh failed" -ForegroundColor Red; exit 1 }

ssh $Target "chmod +x ${AppDir}/deploy.sh"

# ── 3. Run deploy.sh on the VPS ───────────────────────────────────────────────
Write-Host "[3/5] Running deploy.sh on VPS..." -ForegroundColor Cyan
ssh $Target "bash ${AppDir}/deploy.sh"
if ($LASTEXITCODE -ne 0) { Write-Host "Deploy script failed" -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "[4/5] Done! Backend at http://${VpsHost}" -ForegroundColor Green
Write-Host ""
Write-Host "Tip: run 'ssh ${Target} make -C ${AppDir} logs-app' to follow app logs" -ForegroundColor DarkGray
