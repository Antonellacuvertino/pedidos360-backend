# Separacion de microservicios en EC2

Estado al 14 de octubre de 2026: **desplegado en AWS y en pruebas**. Los siete
target groups del ALB estan `healthy` y el VPC Link esta `AVAILABLE`. Las rutas
GET/POST estan creadas en API Gateway; falta confirmar desde el navegador el
flujo de compra completo con un JWT real antes de darlo por cerrado.

## Lo que pide la evaluacion

EP3/EP4 piden microservicios, API Gateway, JWT, dos nodos RabbitMQ en cluster,
tres colas con DLQ, exchanges direct/topic y una demostracion con Docker Compose.
La pauta escrita no exige un tipo de instancia ni una EC2 por servicio. El docente
pidio adicionalmente esa separacion; esta propuesta usa `t3.large` (x86) para
cada microservicio y para el BFF. No usa `m6g.xlarge` porque es ARM y exige
una AMI e imagenes compatibles.

## Topologia objetivo

```text
Navegador -> Angular/Caddy en EC2 existente
          -> API Gateway HTTP + JWT Authorizer de Entra ID
          -> VPC Link -> ALB interno (rutas y target groups)
                         |-> EC2 ms-productos:8081
                         |-> EC2 ms-clientes:8082
                         |-> EC2 ms-pedidos:8083
                         |-> EC2 notifications-service:8084
                         |-> EC2 rabbit-admin-service:8085
                         |-> EC2 ms-auditoria:8086
                         `-> EC2 bff-pedidos360:8080 (resumen y demo)

EC2 existente: PostgreSQL y dos contenedores RabbitMQ en un cluster.
Cada microservicio valida de nuevo el JWT con Spring Security.
```

Se crearon siete EC2 adicionales para separar los seis microservicios y el BFF. Una
`t3.large` cuesta aproximadamente USD 0.0832/h en us-east-1; siete suman
aproximadamente USD 0.58/h de computo, sin EBS, ALB, transferencia ni la EC2
existente. Revisar saldo y cuotas del Learner Lab antes de crearlas; detener
las instancias cuando termine la demostracion. El ALB y los volumenes EBS
siguen generando costos aunque las EC2 esten detenidas.

## Rutas del API Gateway

Crear rutas explicitas en la **API HTTP**. Cada una usa el JWT Authorizer y
el scope indicado. El ALB interno envia el path transformado al target group
del servicio. Conservar el BFF para `resumen` y los endpoints de demostracion.
Las rutas explicitas tienen prioridad sobre la ruta proxy existente. En AWS la
HTTP API es `2iguro8kei`, el VPC Link es `2bstdy` y el ALB interno se llama
`pedidos360-interno`. La ruta `ANY /api/{proxy+}` sigue activa para endpoints
no migrados.

| Ruta externa | Destino | Path interno | Scope |
| --- | --- | --- | --- |
| `GET /api/v1/productos` | ms-productos | `/productos` | `pedidos.read` |
| `POST /api/v1/productos` | ms-productos | `/productos` | `pedidos.write` |
| `GET /api/v1/clientes` | ms-clientes | `/clientes` | `pedidos.read` |
| `POST /api/v1/clientes` | ms-clientes | `/clientes` | `pedidos.write` |
| `GET /api/v1/pedidos` | ms-pedidos | `/pedidos` | `pedidos.read` |
| `POST /api/v1/pedidos` | ms-pedidos | `/pedidos` | `pedidos.write` |
| `GET /api/v1/auditoria` | ms-auditoria | `/auditoria` | `pedidos.read` |
| `GET /api/v1/rabbitmq/queues` | rabbit-admin-service | `/api/rabbitmq/queues` | `pedidos.read` |
| `POST /api/v1/rabbitmq/queues` | rabbit-admin-service | `/api/rabbitmq/queues` | `pedidos.write` |
| `POST /api/v1/notificaciones/prueba` | notifications-service | `/api/email/enviar` | `pedidos.write` |
| `GET /api/v1/resumen` | BFF | `/api/v1/resumen` | `pedidos.read` |
| `GET /api/v1` | BFF | `/api/v1` | `pedidos.read` |
| `POST /api/v1` | BFF | `/api/v1` | `pedidos.write` |
| `GET /api/v1/public` | BFF | `/api/v1/public` | publico |

Cada integracion HTTP privada usa VPC Link y el listener del ALB. El mapeo
`overwrite:path` envia el path interno; para el BFF usa `$request.path` y asi
quita el prefijo de etapa. El ALB tiene una regla por path interno y un target
group por EC2/puerto, con health check `/actuator/health`. Las rutas de
administracion con parametros, como
`DELETE /api/v1/rabbitmq/queues/{name}`, pueden permanecer en el BFF hasta
configurar y probar su mapeo dinamico. No quitar la ruta proxy anterior antes
de validar las rutas nuevas con token real.

Referencias AWS:

- VPC Link e integraciones privadas: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-private.html
- Mapeo de paths: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-parameter-mapping.html
- Prioridad de rutas: https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-routes.html

## Red y seguridad

- API Gateway es el unico endpoint de API que debe usar Angular.
- ALB interno: aceptar trafico solo desde el security group del VPC Link.
- EC2 de cada servicio: aceptar su puerto solo desde el security group del ALB.
- PostgreSQL 5432: aceptar solo desde los security groups de servicios que usan BD.
- RabbitMQ 5672/5673: aceptar solo desde los security groups consumidores/productores.
- RabbitMQ Management 15672/15673: no abrir al publico; usar tunel SSH.
- SSH 22: solo desde la IP del estudiante. No publicar claves ni contrasenas.
- API Gateway y cada Spring Resource Server deben validar `iss`, `aud`, firma,
  vigencia y el scope de las operaciones. Conservar CORS para el dominio HTTPS
  actual del frontend.

## Arranque por EC2

En cada una de las seis EC2 de microservicios, clonar el repositorio backend,
instalar Docker/Compose y configurar `deploy/.env.node` a partir de
`deploy/.env.microservice.example`. Cambiar `SERVICE_NAME`, `SERVICE_PORT`, IP
privada de PostgreSQL, direcciones de RabbitMQ y secretos reales. El archivo
`.env.node` esta ignorado por Git.

```bash
docker compose --env-file deploy/.env.node -f deploy/compose.microservice.yml config --quiet
docker compose --env-file deploy/.env.node -f deploy/compose.microservice.yml up --build -d
docker compose --env-file deploy/.env.node -f deploy/compose.microservice.yml ps
```

En la EC2 del BFF, usar `deploy/.env.bff.example` y las IP privadas de los
seis servicios. El BFF no debe usar URLs publicas entre servicios.

```bash
docker compose --env-file deploy/.env.node -f deploy/compose.bff.yml config --quiet
docker compose --env-file deploy/.env.node -f deploy/compose.bff.yml up --build -d
```

Ya se verifico `curl http://localhost:PUERTO/actuator/health` con `UP` en las
siete EC2, conectividad del BFF a los seis servicios y estado `healthy` en
los siete target groups. Las rutas protegidas probadas sin JWT devolvieron
`401`. Falta probar GET/POST con un JWT real desde Angular o Postman. Una vez
confirmado, se pueden detener los contenedores antiguos de microservicios en
la EC2 base; PostgreSQL, RabbitMQ y Caddy deben seguir activos durante la demo.

## Apagar hoy y encender manana

Con las credenciales temporales de AWS Academy configuradas en PowerShell,
desde este repositorio ejecutar:

```powershell
.\deploy\control-instancias.ps1 -Accion Estado
.\deploy\control-instancias.ps1 -Accion Detener
.\deploy\control-instancias.ps1 -Accion Iniciar
```

`Detener` para primero los siete servicios y luego la EC2 base. `Iniciar`
arranca primero la base y despues las siete EC2. La base tiene la Elastic IP
`100.48.142.195`, por lo que la URL `https://100-48-142-195.sslip.io` se
mantiene tras reiniciarla. Esperar unos minutos hasta que todos los target
groups vuelvan a `healthy`; un estado EC2 `running` no garantiza que Spring
Boot y RabbitMQ ya hayan arrancado.

## Mostrar RabbitMQ al profesor

En AWS Console -> EC2 -> Instancias, mostrar la EC2 que aloja el cluster.
Seleccionar la instancia y usar Conectar -> EC2 Instance Connect si el Learner
Lab ofrece esa opcion. Si no se dispone del archivo `.pem`, ejecutar en
PowerShell con credenciales temporales de AWS Academy ya configuradas:

```powershell
.\deploy\conectar-rabbitmq.ps1 -CopiarClave
```

El script usa una llave de EC2 Instance Connect valida durante 60 segundos,
habilita SSH solo desde la IP publica actual y revoca esa regla al cerrar la
terminal. Mantener la terminal abierta mientras se muestra
`http://localhost:15672`. El script muestra el usuario y deja la clave en el
portapapeles sin imprimirla; pegarla en el login y limpiar el portapapeles al
terminar. Desde la terminal SSH, verificar:

```bash
cd /opt/pedidos360/backend
docker compose ps rabbitmq-1 rabbitmq-2
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose exec rabbitmq-1 rabbitmqctl list_queues name messages consumers
docker compose exec rabbitmq-1 rabbitmqctl list_exchanges name type
docker compose exec rabbitmq-1 rabbitmqctl list_bindings
```

En `cluster_status` deben aparecer `rabbit@rabbitmq-1` y
`rabbit@rabbitmq-2`. En `list_queues`, ubicar las tres colas principales y sus
DLQ. En `list_exchanges`, senalar `orders.exchange` (topic) y
`notifications.exchange` (direct). Los contadores pueden quedar en cero si
los mensajes ya se consumieron; eso no significa que la cola no funcione.

En `http://localhost:15672`, iniciar sesion con el usuario RabbitMQ
configurado en el servidor. Las pestanas Overview, Queues y Exchanges permiten
mostrar los dos nodos, colas, DLQ, exchanges y bindings. No mostrar la
contrasena durante la presentacion.

Documentacion oficial RabbitMQ:
https://www.rabbitmq.com/docs/management y
https://www.rabbitmq.com/docs/man/rabbitmqctl.8

Conexion desde la consola EC2:
https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/connect-linux-inst-eic.html
