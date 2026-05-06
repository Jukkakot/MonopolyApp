# Käynnistää standalone-sessioserverin.
# Käyttö:  .\scripts\start-server.ps1 [portti]
# Oletus:  portti 8080

param(
    [int]$Port = 8080
)

$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "Käynnistetään sessioserveri portissa $Port ..."
Write-Host "Luo sessioita: POST http://localhost:$Port/sessions"
Write-Host "Listaa sessiot: GET  http://localhost:$Port/sessions"
Write-Host "Lopeta: Ctrl+C"
Write-Host ""

mvn -q exec:java `
    -Dexec.mainClass="fi.monopoly.server.session.StartSessionServer" `
    -Dexec.args="$Port"
