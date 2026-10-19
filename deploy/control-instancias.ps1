param(
    [ValidateSet('Estado', 'Iniciar', 'Detener')]
    [string]$Accion = 'Estado',
    [string]$Region = 'us-east-1'
)

$ErrorActionPreference = 'Stop'
$baseId = 'i-0fa2cb315c7d87354'
$servicios = @(
    'i-0e4520b1a0a880a54', # productos
    'i-0488000741bdfc0a4', # clientes
    'i-0981683737994def0', # pedidos
    'i-0e66adb0f0d5b521e', # notificaciones
    'i-041fc61b09e8f5da6', # administracion RabbitMQ
    'i-0bbd4112eeb23a444', # auditoria
    'i-0bb1bdbc28eb2062d'  # BFF
)

function Invoke-Aws([string[]]$Arguments) {
    & aws @Arguments --region $Region --no-cli-pager
    if ($LASTEXITCODE -ne 0) { throw "Fallo AWS CLI: $($Arguments[0]) $($Arguments[1])" }
}

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) { throw 'Instala AWS CLI primero.' }
Invoke-Aws @('sts', 'get-caller-identity', '--query', 'Account', '--output', 'text')

switch ($Accion) {
    'Iniciar' {
        Invoke-Aws @('ec2', 'start-instances', '--instance-ids', $baseId, '--query', 'StartingInstances[].InstanceId', '--output', 'text')
        Invoke-Aws @('ec2', 'wait', 'instance-running', '--instance-ids', $baseId)
        Invoke-Aws (@('ec2', 'start-instances', '--instance-ids') + $servicios + @('--query', 'StartingInstances[].InstanceId', '--output', 'text'))
        Invoke-Aws (@('ec2', 'wait', 'instance-running', '--instance-ids') + $servicios)
        Write-Host 'EC2 iniciadas. Espera 2-3 minutos y comprueba la salud de los servicios.'
    }
    'Detener' {
        Invoke-Aws (@('ec2', 'stop-instances', '--instance-ids') + $servicios + @('--query', 'StoppingInstances[].InstanceId', '--output', 'text'))
        Invoke-Aws (@('ec2', 'wait', 'instance-stopped', '--instance-ids') + $servicios)
        Invoke-Aws @('ec2', 'stop-instances', '--instance-ids', $baseId, '--query', 'StoppingInstances[].InstanceId', '--output', 'text')
        Invoke-Aws @('ec2', 'wait', 'instance-stopped', '--instance-ids', $baseId)
        Write-Warning 'Las EC2 estan detenidas. ALB y volumenes EBS siguen generando costos.'
    }
}

$allIds = @($baseId) + $servicios
$json = & aws ec2 describe-instances --instance-ids $allIds --region $Region --output json --no-cli-pager
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar el estado final.' }
$instances = ($json | ConvertFrom-Json).Reservations.Instances
$instances | ForEach-Object {
    [pscustomobject]@{
        Servicio = ($_.Tags | Where-Object Key -eq 'Name' | Select-Object -First 1).Value
        Estado = $_.State.Name
        IPPrivada = $_.PrivateIpAddress
        IPPublica = $_.PublicIpAddress
    }
} | Format-Table -AutoSize

$base = $instances | Where-Object InstanceId -eq $baseId
if ($Accion -eq 'Iniciar' -and $base.PublicIpAddress -ne '100.48.142.195') {
    Write-Warning 'La IP publica de la EC2 base cambio. La URL sslip.io y el redirect de Entra deben actualizarse antes del login.'
}
