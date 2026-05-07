# push_to_vps.ps1 — Run from Windows to deploy the backend to the VPS
# Usage:  .\push_to_vps.ps1 -User root
# Prereqs: OpenSSH client (comes with Windows 10/11) and git

param(
    [string]$User = "root",
    [string]$Host = "79.143.89.188",
    [string]$AppDir = "/opt/bus_unab"
)

$Target = "${User}@${Host}"

Write-Host "=== Bus UNAB — Push to VPS ===" -ForegroundColor Green

# ── 1. Push latest commits to GitHub first ────────────────────────────────────
Write-Host "[1/4] Pushing to GitHub..." -ForegroundColor Cyan
git push
if ($LASTEXITCODE -ne 0) { Write-Host "git push failed" -ForegroundColor Red; exit 1 }

# ── 2. Copy .env.production (not in git) ─────────────────────────────────────
Write-Host "[2/4] Copying .env.production to VPS..." -ForegroundColor Cyan
scp .env.production "${Target}:${AppDir}/.env.production"
if ($LASTEXITCODE -ne 0) {
    Write-Host "SCP failed. Make sure SSH access works: ssh ${Target}" -ForegroundColor Red
    exit 1
}

# ── 3. Run deploy.sh on the VPS ───────────────────────────────────────────────
Write-Host "[3/4] Running deploy.sh on VPS..." -ForegroundColor Cyan
ssh $Target "bash ${AppDir}/deploy.sh"
if ($LASTEXITCODE -ne 0) { Write-Host "Deploy script failed" -ForegroundColor Red; exit 1 }

Write-Host ""
Write-Host "[4/4] Done! Backend at http://${Host}" -ForegroundColor Green
