# Presentación EP6 · Pedidos360

## Antes de compartir pantalla

- Abre [Pedidos360](https://100-52-32-209.sslip.io) e inicia sesión con Microsoft.
- Abre la consola AWS en **Norte de Virginia (us-east-1)** y prepara **EC2**, **API Gateway → Pedidos360-EP6** y el ALB `p360-ep6-interno`.
- Deja abiertas [Kafka UI](https://kafka.100-52-32-209.sslip.io) y [RabbitMQ](https://rabbit.100-52-32-209.sslip.io).
- En VS Code, abre `Pedidos360.code-workspace` desde la carpeta que contiene ambos repositorios. Para explicar el código basta con tener a mano `deploy/compose.kafka.yml`, `ProductViewPublisher.java`, `AnalyticsConsumer.java` y `PaymentVelocity.java`.

### Entrar a Kafka UI

Usuario: **`pedidos360`**. En PowerShell, desde la carpeta padre de `backend-pedidos360`:

```powershell
.\backend-pedidos360\deploy\copiar-clave-panel-ep6.ps1 -Panel Kafka
```

Pega lo que quedó en el portapapeles en la ventana de autenticación de Kafka UI. Para RabbitMQ usa el mismo script con `-Panel Rabbit`; la clave es distinta. Ninguna se muestra en la terminal. Cierra la terminal o limpia el portapapeles con `Set-Clipboard -Value ''` antes de presentar.

### Dejar Postman listo

1. En Windows, presiona **Inicio**, escribe **Postman** y abre la aplicación de escritorio. Entra a **My Workspace**. Si aparece el aviso de formato por estar en **Local Mode**, usa un workspace normal e importa los JSON; no abras la carpeta completa como repositorio local.
2. Pulsa **Import** e importa estos dos archivos de `backend-pedidos360/postman`: [colección Pedidos360 EP6](../postman/Pedidos360-EP6.postman_collection.json) y [entorno AWS](../postman/Pedidos360-EP6-AWS.postman_environment.json).
3. Arriba a la derecha, selecciona **Pedidos360 EP6 AWS**. `api_gateway_url` ya contiene la dirección del Gateway.
4. En Pedidos360 abre **Mi cuenta** y pulsa **Copiar token**. En Postman abre **Environments → Pedidos360 EP6 AWS** y pega el access token en el valor **local** de `access_token`, sin anteponer `Bearer`. No pulses **Share**, no exportes ese valor y no muestres el token. Repite este paso si la sesión vence.
5. Deja abiertas las solicitudes **01 Público**, **02 Productos sin token**, **03 Productos con token** y **06 Pago simulado**. Usa **Send** por separado; no ejecutes toda la colección porque incluye escrituras.

| Solicitud | Qué mostrar |
| --- | --- |
| 01 Público | `200` sin sesión. |
| 02 Productos sin token | `401`, incluso si el entorno ya tiene un token. |
| 03 Productos con token | `200` y el catálogo JSON. |
| 04 Detalle de producto y 05 Recomendaciones | `200`; la vista publica un evento y la sugerencia puede tardar unos segundos. Cambia `producto_id` por un ID que aparezca en el catálogo. |
| 06 Pago simulado | La tarjeta `0000000000000001` devuelve `200 → 200 → 429` en tres envíos en menos de diez segundos, sin crear pedidos. |
| 07 Estado de tarjeta | Después del bloqueo, `200` con `bloqueado: true` hasta el vencimiento. `tarjeta_id` es la última cifra. |
| Tópicos y grupos Kafka | Mostrar los tópicos, un grupo consumidor y sus offsets. |

La carpeta **Administración Kafka** solo modifica un tópico de prueba. Con `Pedidos.Operador` se espera `403` al crear; con `Pedidos.Admin`, `201`. Luego modifica la retención y elimina ese tópico. No uses nombres de negocio.

[Importar en Postman](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data) · [Valores locales de variables](https://learning.postman.com/docs/use/send-requests/variables/variables/)

## Orden sugerido (8 minutos)

| Tiempo | Qué mostrar | Qué decir |
| --- | --- | --- |
| 0:00 a 1:15 | EC2, API Gateway, Routes y Authorization | «Angular se sirve desde EC2. Las llamadas de datos pasan por Gateway, VPC Link y balanceador interno. Entra emite el token; Gateway valida scopes y el backend controla los roles». |
| 1:15 a 2:00 | Postman: público, sin token, con token | «Así se comprueba que el catálogo no está abierto sin autenticación». |
| 2:00 a 3:30 | Productos, sugerencia, carrito y comprobante | Agrega un notebook. Muestra **Para ti** y paga con la tarjeta terminada en `01`. «No hay cobro real; el comprobante confirma que se recibió la solicitud». |
| 3:30 a 5:00 | Kafka UI: Brokers, Topics, Messages y Consumer Groups | Muestra tres brokers y `product-views` y `payment-attempts`, cada uno con tres particiones y RF 3. Ubica un mensaje: key, partición, offset y valor. En grupos, el lag indica cuánto queda por consumir. |
| 5:00 a 6:00 | Carrito: alerta de fraude | Agrega otro producto y escribe la tarjeta terminada en `02`. Pulsa **Pagar pedido** tres veces dentro de diez segundos. Espera `402 → 402 → 429`, muestra la hora de reintento y que no se generó otro pedido. El bloqueo de 60 segundos es por tarjeta. |
| 6:00 a 7:15 | RabbitMQ: Overview, Queues y Exchanges | Muestra dos nodos, órdenes/stock/notificaciones/auditoría y sus DLQ. «RabbitMQ procesa tareas; Kafka conserva actividad para consultar y volver a procesar». |
| 7:15 a 8:00 | GitHub y alcance | Abre el README y el [informe EP6](INFORME_EP6.md). «La demo usa el tercer intento, más estricto que el cuarto indicado. No integra Webpay ni servicios optativos». |

Si te atrasas al presionar **Pagar pedido**, vuelve a empezar después de diez segundos. Si la tarjeta ya se bloqueó, espera la hora de la alerta o usa otra terminación par para ensayar. No pruebes el bloqueo con `01` antes de mostrar la compra aprobada.
