param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d{12}$')]
    [string]$CuentaEsperada,
    [string]$Region = 'us-east-1'
)
$ErrorActionPreference = 'Stop'
$identityJson = & aws sts get-caller-identity --output json --no-cli-pager
if ($LASTEXITCODE -ne 0) { throw 'Configura las credenciales del laboratorio nuevo en tu equipo.' }
$identity = $identityJson | ConvertFrom-Json
if ($identity.Account -ne $CuentaEsperada) { throw 'La cuenta activa no coincide. No continuar con el despliegue.' }
Write-Host "Cuenta verificada: $($identity.Account). Region: $Region. Solo lectura."
& aws ec2 describe-instances --region $Region --query 'Reservations[].Instances[].{Id:InstanceId,Estado:State.Name,Tipo:InstanceType,Privada:PrivateIpAddress}' --output table --no-cli-pager
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar EC2.' }
& aws apigatewayv2 get-apis --region $Region --query 'Items[].{Id:ApiId,Nombre:Name,Endpoint:ApiEndpoint}' --output table --no-cli-pager
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar API Gateway.' }
Write-Host 'Revisa saldo y cuotas en AWS Academy antes de crear instancias. No se modifico ningun recurso.'
