param([int]$Port=8767)
$ErrorActionPreference='Stop'
if (-not (Get-Command python -ErrorAction SilentlyContinue)) { throw 'Python 3 is required.' }
if (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue) { throw "Port $Port is in use. Open the existing preview or choose another -Port." }
$logRoot=Join-Path $PSScriptRoot 'verification'
$process=Start-Process -FilePath (Get-Command python).Source -ArgumentList @('-m','http.server',"$Port",'--bind','127.0.0.1') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot 'server-out.log') -RedirectStandardError (Join-Path $logRoot 'server-error.log') -PassThru
Start-Sleep -Milliseconds 500
if ($process.HasExited) { throw 'Preview failed; inspect verification/server-error.log.' }
$url="http://127.0.0.1:$Port/prototype/"
if ((Invoke-WebRequest $url).StatusCode -ne 200) { throw 'Preview readback failed.' }
Write-Output "Konoha preview: $url (PID $($process.Id))"
