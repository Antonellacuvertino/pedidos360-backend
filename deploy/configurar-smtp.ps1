param(
    [string]$Region = 'us-east-1',
    [string]$InstanceId = 'i-0e66adb0f0d5b521e',
    [string]$Remitente = 'antonellaroxanna@gmail.com'
)

$ErrorActionPreference = 'Stop'
$keyPath = Join-Path $env:TEMP ("pedidos360-smtp-{0}" -f [guid]::NewGuid().ToString('N'))
$knownHosts = "$keyPath.known_hosts"
$ruleAdded = $false
$cidr = $null
$securityGroup = $null

function Invoke-AwsJson([string[]]$Arguments) {
    $result = & aws @Arguments --region $Region --output json --no-cli-pager
    if ($LASTEXITCODE -ne 0) { throw "AWS CLI fallo: $($Arguments[0]) $($Arguments[1])" }
    return $result | ConvertFrom-Json
}

try {
    foreach ($command in @('aws', 'ssh', 'ssh-keygen', 'icacls')) {
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Falta instalar $command." }
    }
    if ($Remitente -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') { throw 'Indica un correo remitente valido.' }

    $identity = Invoke-AwsJson @('sts', 'get-caller-identity')
    Write-Host "Cuenta AWS: $($identity.Account)"
    $instance = Invoke-AwsJson @('ec2', 'describe-instances', '--instance-ids', $InstanceId)
    $node = $instance.Reservations[0].Instances[0]
    if ($node.State.Name -ne 'running' -or -not $node.PublicIpAddress) {
        throw 'La EC2 de notificaciones debe estar encendida y tener IP publica.'
    }
    $securityGroup = $node.SecurityGroups[0].GroupId

    $publicIp = (Invoke-RestMethod 'https://checkip.amazonaws.com').Trim()
    if ($publicIp -notmatch '^\d{1,3}(\.\d{1,3}){3}$') { throw 'No se pudo determinar la IPv4 publica.' }
    $cidr = "$publicIp/32"
    $group = Invoke-AwsJson @('ec2', 'describe-security-groups', '--group-ids', $securityGroup)
    $alreadyAllowed = @($group.SecurityGroups[0].IpPermissions | Where-Object {
        $_.IpProtocol -eq 'tcp' -and $_.FromPort -le 22 -and $_.ToPort -ge 22 -and
        @($_.IpRanges.CidrIp) -contains $cidr
    }).Count -gt 0
    if (-not $alreadyAllowed) {
        Invoke-AwsJson @('ec2', 'authorize-security-group-ingress', '--group-id', $securityGroup,
            '--protocol', 'tcp', '--port', '22', '--cidr', $cidr) | Out-Null
        $ruleAdded = $true
    }

    & ssh-keygen -q -t ed25519 -N '""' -f $keyPath
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar la llave temporal.' }
    & icacls $keyPath /inheritance:r /grant:r "${env:USERNAME}:(R)" /remove:g '*S-1-3-4' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron restringir los permisos de la llave.' }
    $password = Read-Host 'Pega aqui la contrasena de aplicacion Gmail (no se mostrara)' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    try {
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) -replace '\s', ''
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    if (-not $plainPassword) { throw 'La contrasena de aplicacion esta vacia.' }

    $payload = @{ remitente = $Remitente; password = $plainPassword } | ConvertTo-Json -Compress
    $plainPassword = $null
    $encodedPayload = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($payload))
    $payload = $null
    $remoteScript = @'
import base64
import json
import os
from pathlib import Path
import subprocess
import tempfile

data = json.loads(base64.b64decode('__PEDIDOS360_SMTP_PAYLOAD__'))
root = Path('/opt/pedidos360/backend')
env_file = root / 'deploy/.env.node'
updates = {
    'SMTP_ENABLED': 'true',
    'SMTP_HOST': 'smtp.gmail.com',
    'SMTP_PORT': '587',
    'SMTP_USERNAME': data['remitente'],
    'SMTP_PASSWORD': data['password'],
    'SMTP_FROM': data['remitente'],
}
if not env_file.is_file():
    raise SystemExit('No existe deploy/.env.node en la EC2.')
lines = []
for line in env_file.read_text().splitlines():
    key = line.partition('=')[0]
    if key in updates:
        lines.append(key + '=' + updates.pop(key))
    else:
        lines.append(line)
lines.extend(key + '=' + value for key, value in updates.items())
with tempfile.NamedTemporaryFile(mode='w', dir=env_file.parent, prefix='.env.node.', delete=False) as output:
    os.fchmod(output.fileno(), 0o600)
    output.write('\n'.join(lines) + '\n')
    temporary = output.name
os.replace(temporary, env_file)
subprocess.run(['docker', 'compose', '--env-file', 'deploy/.env.node',
                '-f', 'deploy/compose.microservice.yml', 'up', '-d', '--force-recreate', 'app'],
               cwd=root, check=True, timeout=180)
print('SMTP habilitado; contenedor de notificaciones recreado. Probar entrega real antes de presentarla.')
'@
    $remoteScript = $remoteScript.Replace('__PEDIDOS360_SMTP_PAYLOAD__', $encodedPayload)
    $encodedPayload = $null
    $sent = Invoke-AwsJson @('ec2-instance-connect', 'send-ssh-public-key', '--instance-id',
        $InstanceId, '--availability-zone', $node.Placement.AvailabilityZone,
        '--instance-os-user', 'ec2-user', '--ssh-public-key', "file://$keyPath.pub")
    if (-not $sent.Success) { throw 'EC2 Instance Connect no acepto la llave.' }
    $remoteScript | & ssh -T -o BatchMode=yes -o StrictHostKeyChecking=accept-new `
        -o "UserKnownHostsFile=$knownHosts" -i $keyPath "ec2-user@$($node.PublicIpAddress)" 'python3 -'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo configurar SMTP en la EC2. Revisa el error anterior.' }
} finally {
    if ($ruleAdded -and $cidr -and $securityGroup) {
        & aws ec2 revoke-security-group-ingress --group-id $securityGroup --protocol tcp --port 22 `
            --cidr $cidr --region $Region --no-cli-pager | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Warning "Revise manualmente la regla SSH temporal $cidr." }
    }
    Remove-Item -LiteralPath $keyPath, "$keyPath.pub", $knownHosts -Force -ErrorAction SilentlyContinue
}
