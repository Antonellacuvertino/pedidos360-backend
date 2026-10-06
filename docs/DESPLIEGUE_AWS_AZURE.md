# Despliegue Azure y AWS

## Datos que debes tener

```text
AZURE_TENANT_ID=
AZURE_SPA_CLIENT_ID=
AZURE_API_CLIENT_ID=
AZURE_API_SCOPE=api://AZURE_API_CLIENT_ID/pedidos.write
AZURE_API_AUDIENCE=valor exacto del claim aud
AWS_REGION=us-east-1
EC2_PUBLIC_IP=
API_GATEWAY_URL=
FRONTEND_HTTPS_URL=
```

Los client ID no son secretos. No guardar client secrets, contrasenas, access tokens ni credenciales de AWS en GitHub.

## 1. Microsoft Entra ID

### Registrar la API

1. Entra ID > App registrations > New registration.
2. Nombre: `Pedidos360 API`.
3. En `Expose an API`, definir el Application ID URI.
4. Crear scopes delegados `pedidos.read` y `pedidos.write`.
5. Opcional: crear roles `Pedidos.Admin` y `Pedidos.Operador`.

### Registrar la SPA

1. Crear `Pedidos360 Frontend`.
2. En Authentication agregar plataforma Single-page application.
3. Agregar `http://localhost:4200` y `https://100-48-142-195.sslip.io` como URI de la SPA.
4. En API permissions agregar los scopes de `Pedidos360 API`.
5. Conceder consentimiento si la cuenta tiene permisos.

### Verificar el token

Iniciar sesion, copiar el access token y revisar sus claims. Configurar backend y API Gateway usando exactamente los valores reales de `iss` y `aud` del token. El scope debe aparecer en `scp`.

## 2. Preparar EC2

Usar Amazon Linux 2023 o Ubuntu y permitir:

| Puerto | Origen recomendado | Uso |
| ---: | --- | --- |
| 22 | Tu IP | SSH |
| 8080 | API Gateway o temporalmente tu IP | BFF |
| 15672 | Tu IP | Panel RabbitMQ para demostracion |
| 80/443 | Publico, si corresponde | Frontend o proxy |

Instalar Git y Docker. Luego:

```bash
git clone https://github.com/Antonellacuvertino/pedidos360-backend.git
cd pedidos360-backend
cp .env.example .env
nano .env
docker compose up -d --build
docker compose ps
```

Validar:

```bash
curl http://localhost:8080/actuator/health
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose exec rabbitmq-1 rabbitmqctl list_queues name messages consumers
```

## 3. AWS API Gateway

1. Crear una HTTP API.
2. Integrar con `http://EC2_PUBLIC_IP:8080`.
3. Crear ruta `ANY /api/{proxy+}` y las rutas requeridas.
4. Crear JWT Authorizer.
5. Issuer: valor exacto `iss` del token de Entra ID.
6. Audience: valor exacto `aud` del token.
7. Asociar el authorizer a rutas protegidas.
8. Dejar `/api/v1/public` sin authorizer solo para la demostracion.
9. Configurar CORS con la URL HTTPS del frontend y header `Authorization`.
10. Crear stage con auto deploy.

Pruebas minimas:

```text
GET /api/v1 sin token -> 401
GET /api/v1 con token valido -> 200
POST /api/v1/pedidos con token y scope pedidos.write -> 202
```

## 4. Frontend HTTPS

El frontend se sirve desde la misma instancia EC2 en un contenedor Caddy independiente. Caddy entrega Angular por HTTPS y conserva el certificado en volumenes Docker. La IP elastica debe resolver mediante `100-48-142-195.sslip.io` y el security group debe permitir 80/443.

Antes de compilar, completar `environment.prod.ts`:

```ts
apiBaseUrl: 'https://API_ID.execute-api.us-east-1.amazonaws.com',
azure: {
  clientId: 'SPA_CLIENT_ID',
  tenantId: 'TENANT_ID',
  redirectUri: 'https://FRONTEND_HTTPS_URL',
  authority: 'https://login.microsoftonline.com/TENANT_ID',
  apiScopes: ['api://API_CLIENT_ID/pedidos.read', 'api://API_CLIENT_ID/pedidos.write']
}
```

En EC2, clonar el repositorio del frontend y ejecutar:

```bash
git clone https://github.com/Antonellacuvertino/pedidos360-frontend.git
cd pedidos360-frontend
docker compose up -d --build
docker compose ps
```

Agregar el origen HTTPS como redirect URI SPA en Entra ID y como origen CORS en API Gateway/backend. El navegador debe llamar solo al API Gateway, no a los puertos de los microservicios.

## 5. Actualizar el despliegue

```bash
git pull
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 ms-pedidos notifications-service ms-productos
```

## 6. Diagnostico rapido

| Problema | Revision |
| --- | --- |
| Login no abre | `clientId`, tenant y redirect URI de tipo SPA. |
| `AADSTS50011` | Redirect URI no coincide exactamente. |
| `AADSTS650053` | El scope solicitado no existe. Copiar el valor tecnico desde **Exponer una API** con la traduccion del navegador desactivada; una etiqueta traducida no cambia el nombre real del permiso. |
| Dashboard abre pero no aparece token | Confirmar `pedidos.read` y `pedidos.write` en Permisos de API de la SPA, otorgar consentimiento y volver a iniciar sesion. Mostrar el codigo de error de Mi cuenta. |
| API Gateway responde 401 | Comparar `iss` y `aud` reales con el authorizer. |
| Backend responde 401 | Comparar `AZURE_TENANT_ID` y `AZURE_API_AUDIENCE`. |
| Backend responde 403 | Falta scope `pedidos.write` o rol requerido. |
| No baja stock | Revisar `stock.queue`, logs de `ms-productos` y `stock.dlq`. |
| No llega correo | Revisar SMTP o logs de simulacion en `notifications-service`. |
| Nodo 2 no aparece | Revisar cookie, DNS de contenedores y logs de `rabbitmq-2`. |
