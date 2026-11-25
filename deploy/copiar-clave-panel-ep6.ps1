param(
    [ValidateSet('Kafka', 'Rabbit')]
    [string]$Panel = 'Kafka'
)

$ErrorActionPreference = 'Stop'
$archivo = Join-Path $PSScriptRoot '../.deploy-state/secrets.json'
if (-not (Test-Path -LiteralPath $archivo)) {
    throw 'No existe el estado privado de este despliegue en el equipo. No usar claves del laboratorio anterior.'
}
$config = Get-Content -LiteralPath $archivo -Raw | ConvertFrom-Json
$clave = if ($Panel -eq 'Kafka') { $config.panel } else { $config.rabbit }
if ([string]::IsNullOrWhiteSpace($clave)) {
    throw 'No se encontro la clave del panel solicitado.'
}
Set-Clipboard -Value $clave
$clave = $null
$config = $null
Write-Host "Usuario: pedidos360. Clave de $Panel copiada al portapapeles; no se muestra en pantalla."
Write-Host "Al terminar, limpia el portapapeles con: Set-Clipboard -Value ''"
