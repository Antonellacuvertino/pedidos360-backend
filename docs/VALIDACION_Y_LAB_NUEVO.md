# Validacion de fraude, recomendaciones y laboratorio nuevo

## Cambios del 23-11-2026

- El fraude se evalua en cada solicitud de pago, con una ventana MOVIL de
  diez segundos. No es una tarea que espera diez segundos antes de bloquear.
- El tercer intento bloquea al usuario 30 minutos. La identidad se obtiene
  del issuer y oid/sub del JWT; clienteId y token ficticio no permiten evadirla.
- PostgreSQL conserva los intentos y el vencimiento. Se crea la fila con
  `ON CONFLICT` y se bloquea para actualizarla, incluso ante primeros intentos
  concurrentes. Esta consulta requiere PostgreSQL, no H2 en produccion.
- Antes de publicar cada pedido en RabbitMQ, ms-pedidos consulta a ms-pagos
  usando el JWT recibido y POST /pagos/autorizar-compra, que registra el intento.
  El carrito envia POST /pedidos/lote: cuenta un intento para todos sus items.
  Bloqueado: 429 sin publicar. Servicio no disponible:
  503 sin publicar. Las compras aceptadas antes de un bloqueo no se cancelan
  retroactivamente; el flujo actual no es una transaccion distribuida.
- El evento Kafka de pago es para analitica. La decision de bloqueo es
  sincrona en ms-pagos para no depender del retraso de un consumidor.
- ms-analytics es el microservicio de analitica Y recomendaciones. Consume
  product-views y recomienda productos populares de la ultima categoria del
  usuario. Si no hay alternativas, devuelve el ultimo producto seleccionado.
  No se agrego un segundo servicio identico ni WebSocket.
- Angular registra detalle y agregar al carrito. Consulta recomendaciones
  cada cuatro segundos mientras esta en el catalogo; ese polling no genera
  eventos. No se registran clicks irrelevantes como cerrar un menu.

## Topicos y colas

Kafka UI: http://localhost:8088 para la validacion previa. En AWS se usara
una URL HTTPS autenticada o un tunel, no este localhost como entrega.
En el cluster pedidos360, abrir Topics y desactivar el filtro de internos.
Hay cuatro topicos de aplicacion (pueden aparecer otros internos de Kafka):

| Topico | Productor | Consumidor / finalidad |
| --- | --- | --- |
| product-views | ms-productos: detalle y carrito | ms-analytics: recomendaciones |
| payment-attempts | ms-pagos | ms-analytics: intentos y bloqueos |
| product-views-dlt | recuperador de errores | investigacion manual de eventos fallidos |
| payment-attempts-dlt | recuperador de errores | investigacion manual de eventos fallidos |

Los cuatro tienen 3 particiones y 3 replicas. Los dos primeros son los dos
topicos de negocio exigidos. Ver Overview/Partitions para replicas e ISR;
Messages para key, partition, offset y value; Consumers para grupos y lag.
Kafka NO calcula recomendaciones: conserva y entrega los eventos al servicio.

Ninguna cola RabbitMQ se convirtio en topico. Se conservan orders.queue,
stock.queue, notifications.queue, audit.queue y sus cuatro DLQ. RabbitMQ
ejecuta tareas de compra; Kafka agrega historial para analitica. Borrar esas
colas romperia pedidos, stock o correo.

## Evidencias antes de declarar listo

- Pruebas de ventana movil, tercer intento y desbloqueo: automatizadas.
- Prueba de controlador de pedidos bloqueado sin publicar en Rabbit: automatizada.
- Respuestas de servicio fraude 429/503/permitido: prueba HTTP simulada.
- Algoritmo de popularidad: prueba JPA; frontend: build de produccion.
- Validado con PostgreSQL 16 real: tres solicitudes simultaneas generan dos
  aprobaciones y un bloqueo persistido; cambiar cliente/token no lo evita.
  Evidencia: PostgresFraudRuntimeTest, 1 prueba sin fallos.
- Validado con Kafka real y persistencia H2 de prueba: publicacion de ambos
  tipos de evento, consumo por ms-analytics y recomendacion resultante.
  Evidencia: KafkaRuntimeTest. No es una prueba del navegador ni del Gateway.
- Pendiente: recorrido completo Angular -> Gateway -> productor -> Kafka ->
  consumidor -> recomendacion con configuracion del nuevo laboratorio.
- Pendiente: demostrar tres pagos, GET estado bloqueado y POST pedidos 429
  con un JWT real; confirmar ausencia de nuevo mensaje/orden.
- Validado con Kafka real: mensaje invalido a product-views-dlt, bytes
  conservados y procesamiento posterior de evento valido en la misma particion.
  KafkaRuntimeTest: 2 pruebas, 0 fallos, 0 errores, 0 omitidas.
- No confundir pruebas unitarias con validacion cloud. KAFKA_ENABLED y
  kafkaEnabled del frontend se activan solo cuando exista infraestructura.

### Repetir pruebas locales

Las pruebas opt-in NO usan AWS: Kafka en localhost:9092-9094 y PostgreSQL
temporal en localhost:55432, base validation. No apuntarlas a produccion.
La base H2 de analytics se descarta al cerrar el test; los eventos de prueba
siguen visibles en Kafka UI. Los consumer groups pueden quedar sin miembros
despues de terminar las pruebas, lo que no significa que hayan fallado.

```powershell
$env:KAFKA_RUNTIME_TEST='true'
mvn -pl ms-analytics -am test '-Dtest=KafkaRuntimeTest' '-Dsurefire.failIfNoSpecifiedTests=false'
Remove-Item Env:KAFKA_RUNTIME_TEST
```

PostgresFraudRuntimeTest usa una base temporal creada solo para pruebas con
credenciales ficticias. El contenedor pedidos360-validation-db se retiro
al completar la prueba. Los resultados quedan en target/surefire-reports y
no se suben como artefactos de compilacion a Git.

## Desplegar desde cero (no usar IDs historicos)

1. Confirmar cuenta del nuevo Learner Lab, region, saldo, cuotas y permisos.
   Ejecutar `deploy/verificar-laboratorio.ps1 -CuentaEsperada NUEVA_CUENTA`.
   El script es solo de lectura; no crea ni enciende recursos.
2. Preparar VPC/subredes, security groups, EC2 separadas por microservicio,
   frontend, PostgreSQL/RabbitMQ y host Kafka. Presupuestar ANTES de crearlas.
   Tres brokers en una EC2 no equivalen a tolerar la perdida de esa EC2.
3. Crear PostgreSQL y dos nodos Rabbit; levantar Kafka con
   deploy/compose.kafka.yml y KAFKA_EXTERNAL_HOST=IP_PRIVADA_KAFKA.
   Kafka UI solo en loopback/tunel o HTTPS con autenticacion.
4. Desplegar pagos/analytics/admin Kafka usando compose.streaming-service.yml.
   Configurar pepper privado estable y conexiones privadas. Desplegar los
   servicios anteriores y PAGOS_URL en pedidos. Cada servicio tiene su EC2.
5. Crear ALB interno, target groups y VPC Link; HTTP API Gateway con authorizer
   Entra (issuer del tenant y audience de la API). Rutas explicitas por servicio.
   Angular se aloja en EC2 con HTTPS y llama SOLO al Gateway; el Gateway no
   aloja los archivos Angular. No hacen falta dos Gateway para este recorrido.
6. Ademas de las rutas anteriores, configurar:

| Ruta publica | Destino privado | Scope |
| --- | --- | --- |
| POST /api/v1/productos/{id}/interacciones | mismo path, ms-productos:8081 | pedidos.write |
| GET /api/v1/pagos/estado | /pagos/estado, ms-pagos:8087 | pedidos.read |
| POST /api/v1/pagos/simular | /pagos/simular, ms-pagos:8087 | pedidos.write |
| POST /api/v1/pedidos/lote | /pedidos/lote, ms-pedidos:8083 | pedidos.write |
| GET /api/v1/analitica/recomendaciones | /analitica/recomendaciones, ms-analytics:8089 | pedidos.read |

7. Configurar el NUEVO dominio HTTPS en Entra SPA redirect/logout, CORS del
   Gateway y frontend. Sustituir API base URL; no reutilizar la IP antigua.
8. Probar 401/403, compra normal, bloqueo, recomendaciones, grupos Kafka y DLT.
   Solo entonces habilitar la pantalla Kafka y publicar frontend.

No enviar contrasenas o claves por chat. Configurar las credenciales temporales
en el equipo o usar la sesion web autorizada para Claude. No activar AWS aun
para gastar saldo mientras falten las pruebas previas.
