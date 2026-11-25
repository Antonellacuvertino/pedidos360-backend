$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$private = Join-Path $repo '.deploy-state/secrets.json'
$template = Join-Path $repo '.env.example'
$target = Join-Path $repo '.env'

if (Test-Path -LiteralPath $target) {
    throw 'Ya existe .env. No se sobrescribio.'
}
if (-not (Test-Path -LiteralPath $private -PathType Leaf)) {
    throw 'No se encontro el estado privado del despliegue EP6 en este equipo.'
}

$secrets = Get-Content -LiteralPath $private -Raw | ConvertFrom-Json
$keys = [ordered]@{
    KAFKA_UI_PASSWORD = 'panel'
    DB_PASSWORD = 'db'
    RABBITMQ_PASSWORD = 'rabbit'
    RABBITMQ_ERLANG_COOKIE = 'cookie'
    PAYMENT_FINGERPRINT_PEPPER = 'pepper'
}

$lines = Get-Content -LiteralPath $template -Encoding UTF8
foreach ($entry in $keys.GetEnumerator()) {
    $value = $secrets.($entry.Value)
    if ($value -isnot [string] -or $value -cnotmatch '\A[A-Za-z0-9_-]{20,}\z') {
        throw "Falta un valor valido para $($entry.Key) en el estado privado."
    }
    $pattern = '^' + [regex]::Escape($entry.Key) + '='
    if (@($lines | Where-Object { $_ -match $pattern }).Count -ne 1) {
        throw "La plantilla debe definir $($entry.Key) una sola vez."
    }
    $lines = $lines | ForEach-Object {
        if ($_ -match $pattern) { "$($entry.Key)=$value" } else { $_ }
    }
}

$stream = [IO.File]::Open($target, [IO.FileMode]::CreateNew)
try {
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($lines -join "`n") + "`n")
    $stream.Write($bytes, 0, $bytes.Length)
} finally {
    $stream.Dispose()
}

Write-Host 'Archivo backend-pedidos360/.env creado. No se mostraron las claves.'
