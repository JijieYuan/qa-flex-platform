$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot
npm run dev -- --host 0.0.0.0 --port 18181
