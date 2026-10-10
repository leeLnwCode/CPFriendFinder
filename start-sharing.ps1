param([switch]$Public)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$runtimeDir = Join-Path $env:LOCALAPPDATA 'CPFriendFinder\runtime'
New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
function Ready([string]$url) { try { return (Invoke-WebRequest -Uri $url -TimeoutSec 5).StatusCode -eq 200 } catch { return $false } }
$nodeBinary = (Get-Command node -ErrorAction Stop).Source
if (-not (Ready 'http://127.0.0.1:8080/login')) {
  $mavenBinary = (Get-Command mvn -ErrorAction Stop).Source
  Start-Process -FilePath $env:ComSpec -ArgumentList @('/d','/c','mvn -o spring-boot:run') -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeDir 'backend.log') -RedirectStandardError (Join-Path $runtimeDir 'backend-error.log') | Out-Null
  $deadline = (Get-Date).AddSeconds(120)
  while (-not (Ready 'http://127.0.0.1:8080/login')) {
    if ((Get-Date) -ge $deadline) { throw "Backend did not become ready. Check $runtimeDir\backend.log" }
    Start-Sleep -Seconds 2
  }
}
if (-not (Ready 'http://127.0.0.1:3000/health')) {
  Start-Process -FilePath $nodeBinary -ArgumentList @('server.js') -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtimeDir 'gateway.log') -RedirectStandardError (Join-Path $runtimeDir 'gateway-error.log') | Out-Null
  $deadline = (Get-Date).AddSeconds(20)
  while (-not (Ready 'http://127.0.0.1:3000/health')) {
    if ((Get-Date) -ge $deadline) { throw "Gateway did not become ready. Check $runtimeDir\gateway-error.log" }
    Start-Sleep -Seconds 1
  }
}
Write-Output 'Ready: http://localhost:3000/login'
if ($Public) {
  $cloudflared = Join-Path $runtimeDir 'cloudflared.exe'
  if (-not (Test-Path -LiteralPath $cloudflared)) { Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile $cloudflared }
  $tunnelLog = Join-Path $runtimeDir ('tunnel-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
  Start-Process -FilePath $cloudflared -ArgumentList @('tunnel','--url','http://127.0.0.1:3000','--no-autoupdate') -WindowStyle Hidden -RedirectStandardError $tunnelLog -RedirectStandardOutput ($tunnelLog + '.stdout') | Out-Null
  $deadline = (Get-Date).AddSeconds(60)
  do {
    Start-Sleep -Seconds 1
    $match = [regex]::Match([string](Get-Content -LiteralPath $tunnelLog -Raw -ErrorAction SilentlyContinue), 'https://[a-z0-9-]+\.trycloudflare\.com')
    if ($match.Success) {
      $url = $match.Value
      if (Ready ($url + '/login')) { Write-Output "Public link: $url/login"; break }
    }
    if ((Get-Date) -ge $deadline) { throw "Tunnel did not become ready. Check $tunnelLog" }
  } while ($true)
}
