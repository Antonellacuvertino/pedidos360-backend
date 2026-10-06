# Backend Pedidos360

Backend multi-modulo construido con Java 21 y Spring Boot. Incluye seguridad JWT con Microsoft Entra ID, procesamiento asincrono con RabbitMQ, PostgreSQL y despliegue por contenedores.

## Despliegue cloud actual

Recursos creados el 4 de octubre de 2026 en `us-east-1`:

| Recurso | Identificador o URL |
| --- | --- |
| Frontend Angular en EC2 | `https://100-48-142-195.sslip.io` (requiere abrir 80/443 y registrar URI SPA) |
| Sitio anterior en Amplify | `https://main.d1ipad4fvqyxdz.amplifyapp.com` |
| API Gateway | `https://2iguro8kei.execute-api.us-east-1.amazonaws.com` |
| HTTP API | `2iguro8kei` |
| EC2 | `i-0fa2cb315c7d87354` |
| IP elastica EC2 | `100.48.142.195` |
| RabbitMQ nodo 1 | `http://100.48.142.195:15672` |
| RabbitMQ nodo 2 | `http://100.48.142.195:15673` |
| Entra tenant | `e5372bf0-c5e3-4286-887c-79069f209c1f` |
| Entra API client ID | `7d7e6f82-35dc-4fd2-b580-7776c558d963` |

El grupo `pedidos360-ec2-sg` restringe SSH y los paneles RabbitMQ a la IP del estudiante. El puerto `8080` recibe la integracion HTTP de API Gateway. Los puertos internos de los microservicios no estan habilitados en el grupo de seguridad.

Pruebas rapidas del gateway:

```bash
curl https://2iguro8kei.execute-api.us-east-1.amazonaws.com/api/v1/public
curl -i https://2iguro8kei.execute-api.us-east-1.amazonaws.com/api/v1/productos
```

La primera llamada responde `200`; la segunda responde `401` si no se envia un JWT. No se documentan secretos ni credenciales en el repositorio.

## Arquitectura

```text
Angular + MSAL
      |
      | Bearer JWT
      v
AWS API Gateway + JWT Authorizer
      |
      v
BFF Pedidos360 :8080
      |
      +--> Productos :8081 ---- consume stock.queue
      +--> Clientes :8082
      +--> Pedidos :8083 ------ publica order.created / consume orders.queue
      +--> Notificaciones :8084 consume notifications.queue
      +--> Rabbit Admin :8085 - administra colas, exchanges y bindings
      +--> Auditoria :8086 ---- consume audit.queue
                    |
                    v
         RabbitMQ cluster (2 nodos)
                    |
                    v
                PostgreSQL
```

## Modulos

| Modulo | Puerto | Responsabilidad |
| --- | ---: | --- |
| `bff-pedidos360` | 8080 | Punto de entrada unico para frontend y API Gateway. |
| `ms-productos` | 8081 | Catalogo y descuento asincrono de stock. |
| `ms-clientes` | 8082 | Gestion de clientes. |
| `ms-pedidos` | 8083 | Publica y procesa ordenes. |
| `notifications-service` | 8084 | Consume eventos y envia o simula correo. |
| `rabbit-admin-service` | 8085 | API REST para colas, exchanges y bindings. |
| `ms-auditoria` | 8086 | Microservicio adicional; registra eventos de compra. |
| `common-security` | - | Validacion compartida de issuer, audience, firma, scopes y roles. |
| `common-messaging` | - | Eventos y topologia RabbitMQ centralizada. |

## Mensajeria

La topologia se declara en `common-messaging/RabbitTopologyConfiguration.java`. No hay nombres de colas escritos en los controladores.

| Exchange | Tipo | Routing key | Destino |
| --- | --- | --- | --- |
| `orders.exchange` | topic | `order.created` | `orders.queue`, `notifications.queue`, `stock.queue`, `audit.queue` |
| `notifications.exchange` | direct | `notification.email` | `notifications.queue` |
| `dlx.exchange` | direct | nombre de cada DLQ | colas de mensajes fallidos |

| Cola principal | Consumidor | DLQ |
| --- | --- | --- |
| `orders.queue` | `ms-pedidos` | `orders.dlq` |
| `notifications.queue` | `notifications-service` | `notifications.dlq` |
| `stock.queue` | `ms-productos` | `stock.dlq` |
| `audit.queue` | `ms-auditoria` | `audit.dlq` |

Los consumidores usan ACK manual. Cuando el proceso termina se ejecuta `basicAck`; ante error se usa `basicNack` con `requeue=false`, por lo que RabbitMQ mueve el mensaje a su DLQ.

## Flujo de una compra

1. Angular envia `POST /api/v1/pedidos` con JWT.
2. API Gateway y BFF validan el token.
3. `ms-pedidos` publica `order.created` y responde `202 Accepted`.
4. `orders.queue` guarda la orden.
5. `stock.queue` descuenta inventario.
6. `notifications.queue` envia o simula el correo.
7. `audit.queue` registra trazabilidad.
8. Cada consumidor confirma el mensaje con ACK.

## Seguridad

- API Gateway valida el JWT antes de llegar a EC2.
- El BFF y los microservicios vuelven a validar firma, issuer, audience y expiracion.
- Los scopes del claim `scp` se convierten a `SCOPE_*`.
- Los roles del claim `roles` se convierten a `ROLE_*`.
- Escrituras autorizadas con el scope `pedidos.escribe` o roles `Pedidos.Admin` y `Pedidos.Operador`.
- No se guardan contrasenas, tokens ni archivos `.env` en Git.

## Configuracion

Crear el archivo `.env` a partir de `.env.example`:

```bash
cp .env.example .env
```

Variables principales:

```text
AZURE_TENANT_ID=
AZURE_API_AUDIENCE=api://ID_APLICACION_API
CORS_ALLOWED_ORIGINS=https://URL_FRONTEND
DB_NAME=pedidos360
DB_USER=pedidos360
DB_PASSWORD=
RABBITMQ_USER=pedidos360
RABBITMQ_PASSWORD=
RABBITMQ_ERLANG_COOKIE=
```

Correo SMTP real es opcional:

```text
SMTP_ENABLED=true
SMTP_USERNAME=correo
SMTP_PASSWORD=clave_de_aplicacion
SMTP_FROM=correo
NOTIFICATION_TO=correo_de_prueba
```

Si `SMTP_ENABLED=false`, el consumidor procesa el mensaje y registra un correo simulado en logs.

## Ejecutar con Docker

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

Comprobar el cluster:

```bash
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
docker compose exec rabbitmq-1 rabbitmqctl list_queues name messages consumers
```

Paneles RabbitMQ:

```text
Nodo 1: http://HOST:15672
Nodo 2: http://HOST:15673
```

## Endpoints del BFF

| Metodo | Ruta | Resultado |
| --- | --- | --- |
| `GET` | `/api/v1/public` | Prueba publica. |
| `GET` | `/api/v1` | Claims del JWT. |
| `GET` | `/api/v1/productos` | Lista catalogo y stock. |
| `POST` | `/api/v1/pedidos` | Publica una orden y responde 202. |
| `GET` | `/api/v1/pedidos` | Lista ordenes ya procesadas. |
| `POST` | `/api/v1/notificaciones/prueba` | Publica correo de prueba. |
| `GET` | `/api/v1/rabbitmq/queues` | Muestra colas y consumidores. |
| `POST` | `/api/v1/rabbitmq/queues` | Crea una cola validada. |
| `DELETE` | `/api/v1/rabbitmq/queues/{name}` | Elimina una cola dinamica. |
| `GET` | `/api/v1/auditoria` | Lista eventos procesados. |
| `GET` | `/api/v2/resumen` | Ejemplo de versionamiento. |

## Demostrar una DLQ

Enviar un pedido con un `productoId` inexistente. La copia del consumidor de stock falla y RabbitMQ la envia a `stock.dlq`.

```json
{
  "clienteId": 1,
  "productoId": 999999,
  "cantidad": 1,
  "total": 1000,
  "estado": "RECIBIDO",
  "email": "correo@ejemplo.cl"
}
```

Luego comprobar:

```bash
docker compose exec rabbitmq-1 rabbitmqctl list_queues name messages consumers
```

## Compilar y probar

```bash
mvn test
mvn clean package
```

Las pruebas de repositorio usan H2 y no requieren RabbitMQ. El `docker-compose.yml` fue validado con `docker compose config --quiet`.
