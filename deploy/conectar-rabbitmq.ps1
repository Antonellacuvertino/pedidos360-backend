param(
    [string]$InstanceId = 'i-0fa2cb315c7d87354',
    [string]$Region = 'us-east-1',
    [int]$LocalPort = 15672,
    [switch]$Test,
    [switch]$CopiarClave
)

$ErrorActionPreference = 'Stop'
$keyPath = Join-Path $env:TEMP ("pedidos360-eic-{0}" -f [guid]::NewGuid().ToString('N'))
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
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
            throw "Falta instalar $command."
        }
    }

    $identity = Invoke-AwsJson @('sts', 'get-caller-identity')
    Write-Host "Cuenta AWS: $($identity.Account)"

    $instance = Invoke-AwsJson @('ec2', 'describe-instances', '--instance-ids', $InstanceId)
    $node = $instance.Reservations[0].Instances[0]
    if ($node.State.Name -ne 'running' -or -not $node.PublicIpAddress) {
        throw 'La EC2 debe estar encendida y tener IP publica.'
    }
    $securityGroup = $node.SecurityGroups[0].GroupId

    $publicIp = (Invoke-RestMethod 'https://checkip.amazonaws.com').Trim()
    if ($publicIp -notmatch '^\d{1,3}(\.\d{1,3}){3}$') {
        throw 'No se pudo determinar una IPv4 publica valida.'
    }
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

    $sent = Invoke-AwsJson @('ec2-instance-connect', 'send-ssh-public-key', '--instance-id',
        $InstanceId, '--availability-zone', $node.Placement.AvailabilityZone,
        '--instance-os-user', 'ec2-user', '--ssh-public-key', "file://$keyPath.pub")
    if (-not $sent.Success) { throw 'EC2 Instance Connect no acepto la llave.' }

    if ($CopiarClave) {
        $credentials = & ssh -o BatchMode=yes -o StrictHostKeyChecking=accept-new `
            -o "UserKnownHostsFile=$knownHosts" -i $keyPath "ec2-user@$($node.PublicIpAddress)" `
            "grep -E '^(RABBITMQ_USER|RABBITMQ_PASSWORD)=' /opt/pedidos360/backend/.env"
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo leer la configuracion de RabbitMQ.' }
        $userLine = $credentials | Where-Object { $_ -like 'RABBITMQ_USER=*' } | Select-Object -First 1
        $passwordLine = $credentials | Where-Object { $_ -like 'RABBITMQ_PASSWORD=*' } | Select-Object -First 1
        if (-not $userLine -or -not $passwordLine) { throw 'Faltan datos de RabbitMQ en la EC2.' }
        $rabbitPassword = $passwordLine.Substring('RABBITMQ_PASSWORD='.Length)
        Set-Clipboard -Value $rabbitPassword
        Write-Host "Usuario RabbitMQ: $($userLine.Substring('RABBITMQ_USER='.Length))"
        Write-Host 'Clave copiada al portapapeles. Pegala en el panel y limpia el portapapeles al terminar.'
        $sent = Invoke-AwsJson @('ec2-instance-connect', 'send-ssh-public-key', '--instance-id',
            $InstanceId, '--availability-zone', $node.Placement.AvailabilityZone,
            '--instance-os-user', 'ec2-user', '--ssh-public-key', "file://$keyPath.pub")
        if (-not $sent.Success) { throw 'EC2 Instance Connect no acepto la llave para el tunel.' }
    }

    if ($Test) {
        & ssh -o BatchMode=yes -o StrictHostKeyChecking=accept-new -o "UserKnownHostsFile=$knownHosts" `
            -i $keyPath "ec2-user@$($node.PublicIpAddress)" `
            'docker exec backend-rabbitmq-1-1 rabbitmqctl cluster_status'
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la prueba del cluster RabbitMQ.' }
    } else {
        Write-Host "Abriendo SSH y RabbitMQ en http://localhost:$LocalPort"
        Write-Host 'Mantenga esta ventana abierta durante la demostracion. Al salir se eliminan la llave y el permiso temporal.'
        & ssh -tt -o StrictHostKeyChecking=accept-new -o "UserKnownHostsFile=$knownHosts" `
            -i $keyPath -L "${LocalPort}:127.0.0.1:15672" "ec2-user@$($node.PublicIpAddress)"
    }
}
finally {
    if ($ruleAdded -and $cidr -and $securityGroup) {
        & aws ec2 revoke-security-group-ingress --group-id $securityGroup --protocol tcp --port 22 `
            --cidr $cidr --region $Region --no-cli-pager | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Warning "Revise manualmente la regla SSH temporal $cidr." }
    }
    Remove-Item -LiteralPath $keyPath, "$keyPath.pub", $knownHosts -Force -ErrorAction SilentlyContinue
}
