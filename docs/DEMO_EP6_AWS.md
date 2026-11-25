# Pedidos360: demostracion EP6 en AWS

Fecha de verificacion: 25-11-2026 (Chile). Cuenta 767398066771, region us-east-1.
Este documento corresponde al laboratorio nuevo. No usar las IP ni scripts
de conexion del despliegue anterior sin revisar sus identificadores.

## Direcciones

| Vista | Direccion |
| --- | --- |
| Aplicacion Angular | https://100-52-32-209.sslip.io |
| API Gateway | https://l9xrwlo874.execute-api.us-east-1.amazonaws.com |
| Kafka UI | https://kafka.100-52-32-209.sslip.io |
| RabbitMQ | https://rabbit.100-52-32-209.sslip.io |

Usuario de los paneles: `pedidos360`. Las claves son distintas y se conservan
solo en `.deploy-state/secrets.json`, excluido de Git. Desde la raiz del proyecto:

```powershell
./backend-pedidos360/deploy/copiar-clave-panel-ep6.ps1 -Panel Kafka
# Pegar en el dialogo de Kafka UI. Para RabbitMQ:
./backend-pedidos360/deploy/copiar-clave-panel-ep6.ps1 -Panel Rabbit
# Al terminar:
Set-Clipboard -Value ''
```

No desactivar la verificacion de certificados. No compartir ni exportar claves o JWT.

## Distribucion

```text
Navegador -> Angular/Caddy (EC2 frontend) -> Microsoft Entra ID
                      |
                      v JWT
                API Gateway HTTP API
                      |
                 VPC Link -> ALB interno
                      |
          +-----------+---------------------+
          |                                 |
     EC2 core                          EC2 support
     productos, clientes,              BFF, Rabbit Admin,
     pedidos, notificaciones,          pagos, analytics,
     auditoria                         Kafka Admin
          |                                 |
          +--------- EC2 infra --------------+
          |       PostgreSQL + 2 RabbitMQ    |
          +--------- EC2 streaming ----------+
                  3 ZooKeeper + 3 Kafka + UI
```

Son cinco t3.large; cada servicio Java tiene su contenedor y puerto.
Los tres brokers comparten una EC2: se demuestra replicacion entre brokers,
no tolerancia a la perdida de esa maquina. Lo mismo aplica a los dos nodos
RabbitMQ. Las colas classic no se replican automaticamente por tener dos nodos.
Se uso esta distribucion para el presupuesto y restricciones del laboratorio.

## Evidencia comprobada

- Las cinco EC2 running y disponibles por SSM.
- Diez target groups healthy y diez endpoints Actuator con status UP.
- Angular compilado en EC2; HTTPS responde 200 con certificado validado.
- GET `/api/v1/public` por Gateway: 200 y respuesta del BFF.
- GET `/api/v1/productos` sin token: 401.
- La usuaria confirmo login, catalogo y Streaming en la URL nueva.
- La captura de la usuaria muestra una recomendacion de producto; el diseño se ajusto
  para evitar que su boton solape el nombre.
- RabbitMQ: dos nodos running, sin alarmas ni particiones de red.
- Cuatro colas principales con consumidores y cuatro DLQ; exchanges direct y topic.
- ZooKeeper: tres nodos healthy. Kafka: tres brokers activos.
- Dos topicos de negocio con tres particiones y tres replicas; ISR completo.
- Dos grupos consumidores de analytics conectados.
- Kafka UI autenticado: HTTP 200 y cluster ONLINE, tres brokers.
- API Management de RabbitMQ autenticada: HTTP 200, ocho colas y cuatro consumidores.

Pendiente de prueba con la cuenta del usuario en este laboratorio:
compra con comprobante, bloqueo 429 persistente,
rechazo de compra bloqueada, diferencias Admin/Operador y recuperacion DLQ/DLT.
La existencia de una cola o topico no prueba por si sola ese recorrido.

Webpay no esta integrado: los pagos estan identificados como simulados y no
cobran dinero. SMTP esta desactivado en el laboratorio nuevo; no anunciar correo
real hasta configurarlo y comprobar recepcion. El comprobante no es una boleta fiscal.

## Guion de 8 minutos

1. **0:00-0:45, arquitectura.** Mostrar cinco EC2 y el esquema anterior.
   Decir: "La interfaz esta en EC2. Las llamadas a los servicios entran por API Gateway;
   el balanceador interno distribuye las rutas hacia los contenedores".
2. **0:45-1:45, seguridad.** En AWS abrir API Gateway > Pedidos360-EP6 > Routes,
   Integrations y Authorization. Mostrar productos, pagos y analitica, JWT Authorizer,
   issuer y audience. VPC links debe indicar AVAILABLE. En Postman contrastar 401 sin
   token y 200 con access token. En Entra mostrar usuarios, roles y registro SPA.
3. **1:45-3:00, recomendaciones y compra.** Entrar con Microsoft, abrir Productos,
   ver detalle de un articulo y esperar unos segundos. Mostrar recomendaciones,
   agregar un articulo al carrito y confirmar el comprobante. Hacer esto ANTES del fraude.
4. **3:00-4:15, Kafka UI.** Abrir Topics: `payment-attempts` y `product-views`.
   Mostrar tres particiones, RF=3, ISR, mensajes, key, offset y los grupos de consumidores.
   Decir: "Kafka conserva eventos. analytics los procesa y calcula recomendaciones;
   Kafka no recomienda productos por si solo".
5. **4:15-5:30, fraude.** En Streaming o Postman enviar tres pagos en menos de 10 segundos.
   El tercero debe devolver 429. Consultar el estado, intentar comprar y comprobar que
   no se crea una orden. El bloqueo dura 30 minutos; cambiar de pestana o recargar no lo borra.
6. **5:30-6:30, RabbitMQ.** Overview muestra dos nodos. Queues muestra orders,
   notifications, stock y audit con sus DLQ. Exchanges muestra orders.exchange (topic),
   notifications.exchange (direct) y dlx.exchange (direct). Mostrar actividad tras la compra.
7. **6:30-7:30, administracion.** Con Admin crear un topico demo de tres particiones y
   RF=3, modificar retencion o particiones y eliminar SOLO ese topico. Mostrar rechazo
   403 del Operador en una operacion administrativa. No borrar topicos de negocio.
8. **7:30-8:00, cierre.** Mostrar repositorios separados y explicar por que se mantienen
   RabbitMQ y Kafka. Reconocer que Webpay es un extra pendiente.

## Que se mantiene como cola y que va a Kafka

| Flujo | Tecnologia | Motivo |
| --- | --- | --- |
| Procesar pedidos | RabbitMQ orders.queue | Trabajo que se confirma con ACK; falla a DLQ. |
| Enviar correo | RabbitMQ notifications.queue | Tarea concreta; no reenviar un correo por releer todo el historial. |
| Descontar stock | RabbitMQ stock.queue | Procesamiento de la compra, con tratamiento de fallos. |
| Auditoria de compras | RabbitMQ audit.queue | Se conserva el flujo existente; la auditoria queda persistida en BD. |
| Intentos de pago | Kafka payment-attempts | Historial reutilizable para metricas y consumidores independientes. |
| Vistas y seleccion de productos | Kafka product-views | Interacciones que alimentan recomendaciones y pueden reprocesarse. |

No se migraron colas por cambiar de tecnologia. Se agregaron dos flujos de eventos
para los casos que requieren historial. Tambien existen `payment-attempts-dlt` y
`product-views-dlt`: son destinos de errores, no sustituyen los dos topicos de negocio.

El bloqueo se decide sincronicamente en ms-pagos con PostgreSQL y bloqueo de fila.
Es una ventana movil de 10 segundos evaluada en cada intento, NO una tarea que corre
cada 10 segundos. La identidad sale del JWT; cambiar clienteId en el body no evita
el bloqueo. Kafka recibe el evento para su procesamiento asincrono por analytics.

## Postman

Importar `postman/Pedidos360.postman_environment.json` y las dos colecciones de esa carpeta.
Seleccionar entorno **Pedidos360 EP6 AWS**. Copiar el access token de Mi cuenta a
`access_token` solamente en Postman; no exportar el entorno con el token.

- Coleccion general: GET publico, GET sin token, GET protegido, productos y pedidos.
- Coleccion EP5: 01 detalle, 02 metricas, 03 recomendaciones.
- Ejecutar 04 tres veces en menos de 10 segundos; luego 12 (estado) y 13 (compra bloqueada).
- Esperar mas de 10 segundos desde una compra previa antes de empezar la prueba del fraude.
- Un usuario ya bloqueado devolvera 429 desde el primer intento. No borrar la BD para ocultarlo.

## Comprobacion tecnica repetible

Con credenciales AWS temporales configuradas, desde el backend:

```powershell
python deploy/lab_ep6.py --account 767398066771 status
python deploy/lab_ep6.py --account 767398066771 verify
python deploy/lab_ep6.py --account 767398066771 verify-results
```

Requiere boto3. `verify` manda comandos de lectura por SSM; consultar los resultados
cuando terminen. No imprime variables de contenedores ni claves.
No apagar ni resetear el laboratorio durante la presentacion. Detener EC2 al terminar
no elimina el costo del ALB, las IPv4 reservadas y los discos; revisar esos recursos aparte.
