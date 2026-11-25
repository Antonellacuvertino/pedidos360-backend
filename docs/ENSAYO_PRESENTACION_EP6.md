# Ensayo EP6: Pedidos360 en AWS

Abre [Pedidos360](https://100-52-32-209.sslip.io/app/productos). La evidencia
para el profesor es de AWS; las pruebas de codigo locales son solo verificaciones.
El [guion de ocho minutos](DEMO_EP6_AWS.md) lista las consolas, direcciones y pasos.

## Recorrido para encontrar defectos

1. En una ventana privada, inicia sesion con Microsoft. Entra a Productos.
   Debe verse el catalogo sin pulsar Actualizar y sin el aviso del Gateway.
   Al entrar, **Para ti** no debe aparecer. Abrir un detalle registra una vista
   pero no activa ese bloque.
2. Con el carrito vacio, agrega el Notebook Lenovo. Debajo del catalogo
   busca **Para ti**. Un mouse o monitor disponible complementa al notebook;
   no se repite el notebook ni se ofrecen articulos que ya estan en el carrito.
3. En **Carrito > Pago de prueba**, escribe `0000 0000 0000 0001` y presiona
   **Pagar pedido**. Solo entonces se crea la solicitud y aparece el comprobante.
   En el dashboard, verifica que luego aparezca el pedido. El comprobante no
   es una boleta fiscal y el pago es simulado; no digas que integra Webpay.
   En **Analitica > Para ti**, revisa los complementos por nombre, imagen y precio.
4. Prueba el fraude al final con `0000 0000 0000 0002`. Agrega un producto al
   carrito, escribe el numero y pulsa **Pagar pedido** cuatro veces dentro de
   diez segundos, esperando cada respuesta: 402, 402, 402 y 429.
   No se vacia el carrito ni se emite un comprobante. El bloqueo dura 30 minutos
   para ese numero en todas las cuentas. Para ensayar y luego presentar puedes
   usar terminaciones pares distintas (2, 4, 6 u 8); no uses tarjetas reales.
5. Anota el resultado de cada paso. Si algo falla, registra la ruta, hora
   y codigo HTTP en Red/Network, sin capturar Authorization ni el JWT.

## Que mostrar en Kafka UI

Abre [Kafka UI](https://kafka.100-52-32-209.sslip.io). Usa el script para
copiar la clave descrito en [la guia AWS](DEMO_EP6_AWS.md#direcciones).

| Pantalla | Evidencia | Que decir |
| --- | --- | --- |
| Overview / Brokers | 3 brokers y controlador ZooKeeper | "Estos brokers distribuyen los lideres y guardan replicas. Comparten una EC2 de laboratorio, asi que no hay tolerancia a la perdida de ese host". |
| Topics | `product-views` y `payment-attempts`, cada uno con 3 particiones y RF 3 | "Son los dos flujos del negocio. Los sufijos -dlt identifican destinos de errores, no flujos nuevos". |
| product-views > Messages | Un evento CARRITO de tu prueba, con key, partition y offset | "Productos publica la seleccion. Analytics la consume y consulta el catalogo para sugerir un complemento en stock". |
| payment-attempts > Messages | Estados APROBADO_SIMULADO, RECHAZADO_SIMULADO y BLOQUEADO | "Kafka conserva el historial; el bloqueo ocurre antes de confirmar la orden, en Pagos y PostgreSQL". |
| Consumer Groups | `analytics-product-views` y `analytics-payment-attempts`, offsets y lag | "El offset marca el progreso por particion. El lag es la diferencia con el final del registro". |
| Topics / Postman Admin | Crear un topico demo con 3 particiones y RF 3, ajustar retencion y eliminar solo ese topico | "Kafka Admin permite modificar configuracion; Operador no tiene permiso de escritura administrativa". |

Para acreditar el compose, muestra `deploy/compose.kafka.yml` y sus tres
ZooKeeper, tres brokers y Kafka UI. Muestra el estado de contenedores de la
EC2 de streaming mediante la verificacion SSM de la guia, no con Docker local.

## Que dejar abierto en VS Code y GitHub

Abre `Pedidos360.code-workspace` desde la raiz del proyecto. El explorador
muestra los dos repositorios y oculta binarios, caches y configuracion privada
sin eliminarlos. Cierra terminales con credenciales antes de compartir pantalla.

Ten estas pestanas disponibles y abre solo la que corresponda al paso:

| Tema | Archivo |
| --- | --- |
| Kafka y ZooKeeper | `backend-pedidos360/deploy/compose.kafka.yml` |
| Productor | `backend-pedidos360/ms-productos/src/main/java/cl/pedidos360/productos/ProductViewPublisher.java` |
| Consumidor y recomendacion | `AnalyticsConsumer.java` y `RecommendationService.java` en `ms-analytics` |
| Limite de fraude | `PaymentVelocity.java` en `ms-pagos` |
| Pantalla y JWT | `productos.component.ts` y `app.config.ts` en el frontend |

GitHub: [frontend](https://github.com/Antonellacuvertino/pedidos360-frontend)
y [backend](https://github.com/Antonellacuvertino/pedidos360-backend). Empieza
por el README, no por el historial. No muestres archivos de credenciales.

**Cierre sugerido:** "Mantuvimos pedidos, stock, notificaciones y auditoria en
RabbitMQ porque son tareas con confirmacion y DLQ. Agregamos Kafka para el
historial de interacciones y pagos, que puede volver a leerse. AWS API Gateway
valida el token de Entra antes de enrutar cada solicitud a EC2".
