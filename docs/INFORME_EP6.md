# Informe EP6 · Pedidos360

**Desarrollo Cloud Native** · Noviembre de 2026

## 1. Objetivo

En esta entrega agregamos dos cosas al flujo de una tienda que ya registraba pedidos: un pago de prueba que pueda detener intentos repetidos y recomendaciones que cambien según lo que el usuario mira o agrega al carrito. Queríamos mantener la compra separada del análisis: para recibir un pedido no hace falta recalcular métricas ni esperar a que se generen recomendaciones.

## 2. Arquitectura y despliegue

```text
Usuario --> Angular/Caddy en EC2 <-- inicio de sesión --> Microsoft Entra
               |
               | HTTPS + access token
               v
         API Gateway (HTTP API)
               |
         VPC Link --> ALB interno
               |-- EC2 core: Productos, Clientes, Pedidos,
               |             Notificaciones y Auditoría
               |-- EC2 support: BFF, Pagos, Analítica,
                                Rabbit Admin y Kafka Admin
               |
               |-- EC2 infra: PostgreSQL y dos nodos RabbitMQ
               '-- EC2 streaming: tres ZooKeeper, tres brokers y Kafka UI
```

El backend está en Java 21 y Spring Boot; el frontend usa Angular y MSAL. El laboratorio usa cinco instancias en `us-east-1`. Cada servicio Java corre en su propio contenedor, aunque varios comparten EC2. Igual ocurre con los brokers: las réplicas cubren fallos de un broker, pero no la caída de la EC2 que aloja todo Kafka. No presentamos esa topología como alta disponibilidad entre máquinas.

La [web](https://100-52-32-209.sslip.io) se sirve desde EC2. La [API](https://l9xrwlo874.execute-api.us-east-1.amazonaws.com/api/v1/public) recibe las llamadas del frontend. No usamos dos Gateways: el ALB interno distribuye las peticiones a los servicios sin exponer sus puertos a Internet.

| Componente | Responsabilidad |
| --- | --- |
| Productos y Clientes | Consulta y gestión del catálogo y de los clientes. Productos publica eventos de interacción. |
| Pedidos | Verifica el pago antes de enviar `order.created` a RabbitMQ. |
| Pagos | Simula el pago, controla la frecuencia por tarjeta y publica el resultado en Kafka. |
| Analítica | Consume los dos tópicos de negocio y entrega métricas y recomendaciones. |
| Notificaciones y Auditoría | Consumen eventos de pedidos. Auditoría es el microservicio libre. |
| BFF | Construye el resumen del dashboard y ofrece las rutas de prueba de la API. |
| Rabbit Admin y Kafka Admin | Separan la administración de colas y tópicos de las operaciones de venta. |

## 3. Compra y control de fraude

1. El usuario agrega productos al carrito e ingresa un número ficticio. No se reciben tarjetas reales.
2. `POST /api/v1/pedidos/lote` llega a Pedidos. Una compra con varios artículos se cuenta como un intento, no como uno por producto.
3. Pagos identifica la tarjeta mediante una huella HMAC-SHA256. El número de prueba no se guarda en el historial ni se publica en Kafka. PostgreSQL guarda los intentos recientes y el vencimiento del bloqueo; una transacción bloquea la fila para que dos peticiones simultáneas no pierdan el conteo.
4. Para la demo aplicamos un límite más estricto que el enunciado: el **tercer intento de la misma tarjeta en menos de diez segundos** la bloquea durante **60 segundos**. El enunciado dice «más de tres», que literalmente se activa en el cuarto. El bloqueo corresponde al número ficticio, no a la cuenta.
5. Un rechazo de la tarjeta de prueba devuelve `402`; un bloqueo devuelve `429` con la hora de reintento. En los dos casos el carrito conserva sus productos y no se publica un pedido. Ante un pago simulado aprobado, Pedidos publica el evento y responde `202`; la web muestra el comprobante de recepción. Este comprobante no acredita un cobro ni es una boleta fiscal.

La tarjeta terminada en `01` normalmente aprueba. La terminada en `02` simula un rechazo, pero **no viene bloqueada**: sirve para repetir el pago sin volver a llenar el carrito.

## 4. Eventos y recomendaciones

| Tópico | Productor | Consumidor | Para qué se usa |
| --- | --- | --- | --- |
| `product-views` | Productos | `analytics-product-views` | Guarda vistas y selecciones para decidir qué recomendar. |
| `payment-attempts` | Pagos | `analytics-payment-attempts` | Guarda resultados e intentos bloqueados para el panel de actividad. |

Cada tópico de negocio tiene tres particiones y factor de replicación tres. La clave permite enviar los eventos de un mismo usuario (interacción) o de una misma tarjeta (pago) a la misma partición. Los tópicos `product-views-dlt` y `payment-attempts-dlt` reciben errores que agotan los reintentos del consumidor; no son otros flujos de negocio.

Al abrir un detalle se publica `VISTA`; al agregar un artículo se publica `CARRITO`. Carrito forma parte de Angular y usa el mismo endpoint de interacciones de Productos. **No hay un microservicio Java Carrito separado.** Analítica registra los eventos y usa la última interacción relevante: para una vista prioriza artículos de la misma categoría; para una selección propone categorías complementarias. Consulta el stock y excluye el artículo base. Angular tampoco ofrece un artículo que ya está en el carrito.

## 5. Decisión sobre RabbitMQ

Mantuvimos RabbitMQ para tareas que queremos ejecutar, confirmar y, si fallan, enviar a una DLQ.

| Cola | Tarea | DLQ |
| --- | --- | --- |
| `orders.queue` | Registrar la orden | `orders.dlq` |
| `stock.queue` | Descontar inventario | `stock.dlq` |
| `notifications.queue` | Procesar el aviso | `notifications.dlq` |
| `audit.queue` | Guardar el evento de auditoría | `audit.dlq` |

El exchange `orders.exchange` es `topic`; `notifications.exchange` y `dlx.exchange` son `direct`. Kafka queda para eventos que conviene conservar y volver a leer. Por ejemplo, podemos recalcular las métricas sin volver a descontar stock ni enviar notificaciones duplicadas. La decisión de bloqueo se toma en Pagos antes de publicar la orden; no espera a que Analítica consuma Kafka.

## 6. Seguridad y acceso

La HTTP API `Pedidos360-EP6` tiene 33 rutas. Solo `GET /api/v1/public` está sin JWT. Las demás usan el emisor de Microsoft Entra, la audiencia de la API y `pedidos.read` o `pedidos.write`. Spring valida el token nuevamente y exige `Pedidos.Admin` para modificar colas, exchanges, bindings y tópicos. CORS permite el origen del frontend; no reemplaza la autorización con JWT.

Los security groups permiten 80/443 hacia el frontend. PostgreSQL, los brokers y los puertos Java tienen reglas de origen internas. Kafka UI y RabbitMQ Management se abren por HTTPS y requieren autenticación. EC2 exige IMDSv2 y usa el perfil compartido del laboratorio, que es distinto de los roles de Entra. El stage no tiene access logs configurados.

## 7. Pruebas y evidencia

| Verificación | Resultado comprobado |
| --- | --- |
| Construcción | Angular compiló y pasaron las pruebas Maven enfocadas en pagos, pedidos y recomendaciones. |
| Regla de pago | Con la misma tarjeta ficticia, dos aprobaciones y un bloqueo en el tercer intento. Se verificó que 429 no publica el pedido y devuelve el vencimiento. |
| Frontend y API en AWS | El sitio sirvió el bundle actualizado. Ruta pública `200`; productos y checkout sin JWT `401`. Solo el origen de la web recibió la cabecera CORS. |
| Servicios e infraestructura | Cinco EC2 activas, diez destinos del ALB saludables en la revisión, tres brokers Kafka en línea y dos nodos RabbitMQ. |
| Mensajería | Se comprobaron particiones, réplicas e ISR de los dos tópicos, los grupos consumidores y la topología de colas y DLQ. |

Para la exposición conviene repetir el login, la compra y el aviso antifraude con la sesión de demo. Los comandos y las pruebas para mostrar están en la [guía de presentación](PRESENTACION_EP6.md). Una prueba unitaria no reemplaza esa ejecución con JWT real.

## 8. Alcance y conclusión

El pago es simulado. No se integraron Webpay, DynamoDB ni WebSockets; las métricas se consultan por HTTP, y el laboratorio no tiene SMTP real activo. Las imágenes son de referencia. El objetivo fue dejar funcionando el recorrido central y separar el registro de órdenes, el pago y el análisis. Así podemos probar el bloqueo en el checkout y luego explicar en Kafka UI qué interacción o intento generó cada evento.
