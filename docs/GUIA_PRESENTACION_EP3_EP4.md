# Guia de estudio y presentacion EP3 / EP4

Esta guia sigue las pautas oficiales entregadas para Pedidos360. La demostracion debe durar entre 5 y 10 minutos.

## Que debe estar visible antes de comenzar

- URL HTTPS del frontend.
- Tenant y registros de aplicaciones en Microsoft Entra ID.
- API HTTP y JWT Authorizer en AWS API Gateway.
- Instancia EC2 en estado `running`.
- `docker compose ps` con servicios activos.
- Panel RabbitMQ abierto en el nodo 1.
- Postman con `api_gateway_url` y un access token vigente.

## Valores del despliegue

| Elemento | Valor |
| --- | --- |
| Frontend en EC2 | `https://100-48-142-195.sslip.io` (comprobar HTTPS antes de presentar) |
| API Gateway | `https://2iguro8kei.execute-api.us-east-1.amazonaws.com` |
| Tenant ID | `e5372bf0-c5e3-4286-887c-79069f209c1f` |
| Client ID frontend | `ad02ca6f-9972-496e-837c-98c92a43220e` |
| Client ID API | `7d7e6f82-35dc-4fd2-b580-7776c558d963` |
| Audience | `api://7d7e6f82-35dc-4fd2-b580-7776c558d963` |
| Scopes | `pedidos.read`, `pedidos.escribe` |

## Guion de 8 minutos

### 0:00 - Arquitectura

"Pedidos360 usa Angular para la interfaz, Microsoft Entra ID como proveedor de identidad, API Gateway como entrada segura y microservicios Spring Boot desplegados en EC2. Las escrituras de pedidos se procesan de forma asincrona con RabbitMQ."

Muestra el diagrama del README y explica que el frontend nunca llama directamente a los microservicios.

### 0:45 - Azure y autenticacion

En Microsoft Entra ID muestra:

- Tenant.
- Usuario de prueba.
- Registro de la SPA y redirect URI HTTPS.
- Registro de la API y scopes `pedidos.read` y `pedidos.escribe`.

En el frontend inicia sesion con Microsoft y abre `Mi cuenta` para mostrar el token decodificado. Explica `iss`, `aud`, `exp`, `scp` y `roles`.

### 1:45 - API Gateway

Muestra:

- API HTTP.
- Integracion con `http://EC2:8080`.
- Rutas `/api/v1/{proxy+}`.
- CORS.
- JWT Authorizer con issuer y audience.

Ejecuta en Postman una llamada sin token y otra con token. Debe verse `401` y luego `200`.

### 2:45 - Despliegue en EC2

En la terminal de EC2 ejecuta:

```bash
docker compose ps
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
```

Explica que cada microservicio tiene su propio contenedor y puerto. En `cluster_status` deben aparecer `rabbit@rabbitmq-1` y `rabbit@rabbitmq-2`.

### 3:45 - Colas, exchanges y DLQ

En RabbitMQ muestra:

- `orders.exchange` de tipo topic.
- `notifications.exchange` de tipo direct.
- `orders.queue`, `notifications.queue`, `stock.queue` y sus DLQ.
- `audit.queue` como cola del microservicio adicional.

Explica que los nombres y bindings estan centralizados en `RabbitTopologyConfiguration` y que los listeners usan ACK/NACK manual.

### 5:00 - Compra completa

1. Abre Productos.
2. Agrega un producto al carrito.
3. Confirma la compra.
4. Muestra la respuesta `202 Accepted` en Network o Postman.
5. Recarga productos para mostrar menor stock.
6. Consulta `/api/v1/pedidos` y `/api/v1/auditoria`.
7. Muestra el log de notificaciones o el correo recibido.

Frase clave:

"El POST no espera que todos los procesos terminen. Publica el evento y cada consumidor procesa su responsabilidad de forma desacoplada."

### 6:30 - Prueba de DLQ

Envia un pedido con `productoId` inexistente. Luego muestra `stock.dlq` con un mensaje.

```bash
docker compose exec rabbitmq-1 rabbitmqctl list_queues name messages consumers
```

Explica que el consumidor ejecuta `basicNack` con `requeue=false`; el DLX redirige el mensaje para analizarlo sin perderlo.

### 7:30 - Cierre

"La solucion integra identidad, seguridad perimetral, validacion en backend, base de datos, mensajeria resiliente y despliegue reproducible. Un pedido genera persistencia, actualizacion de stock, notificacion y auditoria sin acoplar esos procesos."

## Conceptos para estudiar

| Concepto | Explicacion breve |
| --- | --- |
| IDaaS | Servicio externo de identidad. En el proyecto es Microsoft Entra ID. |
| OAuth 2.0 | Protocolo para obtener permisos de acceso a una API. |
| OpenID Connect | Capa de identidad sobre OAuth 2.0. |
| PKCE | Protege el intercambio del codigo de autorizacion en aplicaciones SPA. |
| MSAL | Libreria de Microsoft para login, logout y obtencion/renovacion de tokens. |
| JWT | Token firmado con claims como issuer, audience, scopes, roles y expiracion. |
| API Gateway | Entrada unica que aplica CORS, rutas y autorizacion JWT. |
| Resource Server | API que valida el Bearer token antes de autorizar una peticion. |
| Exchange direct | Envia por coincidencia exacta de routing key. |
| Exchange topic | Enruta mediante patrones como `order.*` u `order.#`. |
| ACK | Confirma que un consumidor proceso correctamente un mensaje. |
| NACK | Rechaza un mensaje y decide si se reencola o va a DLQ. |
| DLX / DLQ | Exchange y cola usados para conservar mensajes que no pudieron procesarse. |
| Idempotencia | Reprocesar el mismo evento sin duplicar el resultado. |
| BFF | Capa que adapta y centraliza las llamadas del frontend. |

## Preguntas probables

**Por que se valida el JWT dos veces?**

API Gateway protege el perimetro y Spring Security protege el servicio aunque alguien intente acceder directamente a EC2. Es defensa en profundidad.

**Por que el POST devuelve 202 y no 201?**

Porque el pedido fue aceptado, pero su procesamiento continua en segundo plano mediante RabbitMQ.

**Que ocurre si RabbitMQ entrega dos veces el mismo mensaje?**

Pedidos, stock y auditoria guardan el `eventId` procesado. Si vuelve a llegar, no repiten la operacion.

**Por que usar topic y direct?**

Topic permite distribuir un evento de orden a varios dominios. Direct sirve para enviar una notificacion a una ruta exacta.

**Que aporta el microservicio de auditoria?**

Mantiene trazabilidad independiente del flujo de compra sin modificar pedidos ni productos.

**Que diferencia hay entre 401 y 403?**

`401` indica token ausente o invalido. `403` indica token valido, pero sin scope o rol suficiente.

## Lista final de evidencias

- Login Microsoft funcionando.
- JWT visible y decodificado.
- API Gateway rechaza sin token y acepta con token.
- Frontend consume exclusivamente API Gateway.
- EC2 y contenedores activos.
- Dos nodos RabbitMQ en el cluster.
- Exchanges direct y topic.
- Tres colas principales y sus DLQ como minimo.
- ACK/NACK visible en codigo y logs.
- Orden guardada, stock descontado y notificacion procesada.
- Auditoria consultable.
- Repositorios GitHub actualizados y sin secretos.
