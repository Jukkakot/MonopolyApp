# Luo uuden peliession running-serverille ja tulostaa yhteystiedot.
# Käyttö:  .\scripts\new-session.ps1 [Pelaaja1] [Pelaaja2] ...
# Oletus:  Jukka ja Botti, portti 8080

param(
    [string[]]$Players = @("Jukka", "Botti"),
    [int]$Port = 8080,
    [string[]]$Colors = @("#E63946", "#2A9D8F", "#E9C46A", "#264653")
)

$base = "http://localhost:$Port"

# Rakenna JSON
$names  = ($Players | ForEach-Object { "`"$_`"" }) -join ","
$cols   = ($Colors[0..($Players.Length - 1)] | ForEach-Object { "`"$_`"" }) -join ","
$body   = "{`"names`":[$names],`"colors`":[$cols]}"

try {
    $resp = Invoke-RestMethod -Uri "$base/sessions" -Method POST `
                -ContentType "application/json" -Body $body
} catch {
    Write-Error "Serveri ei vastaa portissa $Port. Käynnistä ensin: .\scripts\start-server.ps1"
    exit 1
}

$sid = $resp.sessionId

Write-Host ""
Write-Host "Sessio luotu!"
Write-Host "  Session ID : $sid"
Write-Host "  Pelaajat   : $($Players -join ', ')"
Write-Host ""
Write-Host "Endpointit:"
Write-Host "  Snapshot   : GET  $base/sessions/$sid/snapshot"
Write-Host "  Komennot   : POST $base/sessions/$sid/command"
Write-Host "  SSE-stream : GET  $base/sessions/$sid/events"
Write-Host ""

# Hae aloittava pelaaja snapshotista
$snap = Invoke-RestMethod -Uri "$base/sessions/$sid/snapshot"
$active = $snap.turn.activePlayerId
$phase  = $snap.turn.phase
Write-Host "Vuorossa nyt: $active  (phase: $phase)"
Write-Host ""

# Kopioi session ID leikepöydälle jos mahdollista
$sid | Set-Clipboard
Write-Host "(Session ID kopioitu leikepöydälle)"
