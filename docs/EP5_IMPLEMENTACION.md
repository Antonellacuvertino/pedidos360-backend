# EP5: Kafka en Pedidos360

Revision EP6 del 24-11-2026: prevalece
[DESPLIEGUE_MINIMO_EP5_EP6.md](DESPLIEGUE_MINIMO_EP5_EP6.md).
El primer indicador EP6 requiere tres ZooKeeper: Compose fue actualizado,
pero la nueva topologia aun debe arrancarse y verificarse en AWS.
Las pruebas historicas descritas aqui usaron UN ZooKeeper y tres brokers.

Esta etapa **agrega** streaming a la arquitectura anterior. RabbitMQ sigue
procesando pedidos, stock, notificaciones y auditoria. Kafka conserva eventos
para analitica; no reemplaza esas colas.

Para el laboratorio NUEVO y la revision del bloqueo de compras consultar
[VALIDACION_Y_LAB_NUEVO.md](VALIDACION_Y_LAB_NUEVO.md). Los IDs AWS citados
mas abajo pertenecen al laboratorio anterior, no deben reutilizarse.

## Estado comprobado

- `mvn test` del reactor completo: sin fallos el 23-11-2026.
- `npm run build` del frontend: compila el 23-11-2026; `kafkaEnabled` sigue desactivado.
- `docker compose config --quiet` de los dos archivos Compose nuevos: valido.
- Docker local: ZooKeeper, tres brokers y Kafka UI iniciaron correctamente.
  Kafka UI respondio HTTP 200 en `http://localhost:8088`. `kafka-init`
  creo `payment-attempts` y `product-views` con 3 particiones, 3 replicas
  e ISR de tres nodos; tambien creo sus dos topicos DLT. Los contenedores
  locales se detuvieron despues de la prueba para liberar memoria, sin borrar
  los volumenes. Para repetir la demo, ejecutar `docker compose -f
  deploy/compose.kafka.yml up -d`.
- Publicacion de prueba y consumo real por ms-analytics comprobados con
  KafkaRuntimeTest, incluidos DLT y recuperacion. PostgreSQL real comprobo
  concurrencia del bloqueo con PostgresFraudRuntimeTest.
- **Pendiente:** desplegar las EC2 del laboratorio nuevo,
  rutas del API Gateway y frontend actualizado en AWS.
- No se ha integrado Webpay real. El pago de `ms-pagos` es una simulacion y
  nunca debe presentarse como cobro o boleta tributaria.

## Que pide la pauta y donde esta

| Indicador EP5 | Implementacion | Evidencia por obtener |
| --- | --- | --- |
| Cliente Kafka, serializers y grupos | `ms-productos`, `ms-pagos`, `ms-analytics` | Logs y Kafka UI |
| Dos topicos, productores y consumidores tipados | `product-views`, `payment-attempts`, `common-streaming` | Mensajes con key, partition y offset |
| Errores y reintentos | `ms-analytics/KafkaFailureConfiguration` | Dos reintentos, log y topico `-dlt` |
| Administracion de topicos y grupos | `kafka-admin-service` | GET/POST/PUT/DELETE con JWT |
| Particiones, replicas, retencion | `deploy/kafka-init.sh` | 3 particiones y 3 replicas por topico |
| ZooKeeper, brokers y UI | `deploy/compose.kafka.yml` | `docker compose ps` y Kafka UI |
| RabbitMQ previo | `docker-compose.yml`, `common-messaging` | 2 nodos, colas, DLQ y exchanges |

La guia entregada por el curso muestra un broker con factor de replica 1.
Eso **no cumple** la demostracion solicitada de tres replicas; aqui se usan
tres brokers. Los tres comparten un host en Compose, por lo que resisten la
caida de un contenedor, pero **no** la caida de esa EC2.

## 1. Levantar Kafka en Docker

En la raiz del repositorio backend, con Docker Desktop iniciado:

```powershell
Copy-Item deploy/.env.kafka.example deploy/.env.kafka
docker compose --env-file deploy/.env.kafka -f deploy/compose.kafka.yml config --quiet
docker compose --env-file deploy/.env.kafka -f deploy/compose.kafka.yml up -d
docker compose --env-file deploy/.env.kafka -f deploy/compose.kafka.yml ps
docker compose --env-file deploy/.env.kafka -f deploy/compose.kafka.yml logs kafka-init
```

Kafka UI: `http://localhost:8088`. En **Topics** revisar `payment-attempts`
y `product-views`: 3 particiones, factor de replica 3, retencion de 7 y 3
dias respectivamente. Tambien existen sus topicos `-dlt` para errores. En
**Brokers** deben aparecer tres. `kafka-init` puede terminar con exit code 0:
es una tarea de inicializacion, no un servicio permanente.

En EC2, establecer `KAFKA_EXTERNAL_HOST` con la **IP privada** del host Kafka
y permitir 9092-9094 solo desde los grupos de seguridad de los servicios.
Kafka UI se enlaza a `127.0.0.1:8088`; acceder por tunel SSH o proxy HTTPS
autenticado, nunca abrir 8088 a todo Internet.

## 2. Ejecutar y probar los servicios

Usar Java 21 o superior y Maven 3.9+:

```powershell
mvn test
```

`ms-productos` publica en `product-views` al consultar el detalle
`GET /productos/{id}`. La clave del mensaje es el `oid`/`sub` del JWT, de
modo que las vistas del mismo usuario conservan orden por particion.
Activar `KAFKA_ENABLED=true` solo cuando el cluster este disponible.

`ms-pagos` acepta `POST /pagos/simular` con un token **ficticio** `demo_...`.
Los dos primeros intentos del mismo usuario autenticado en diez segundos se aprueban en
modo simulado; el tercero devuelve 429 y bloquea 30 minutos. No se aceptan
numeros de tarjeta. El token se convierte en huella SHA-256 con un pepper
privado antes de publicar. El estado de fraude se vincula al usuario del JWT,
no al clienteId del formulario. El carrito usa /pedidos/lote para contar una
sola solicitud por compra; /pedidos tambien exige autorizacion de ms-pagos.
Definir
`PAYMENT_FINGERPRINT_PEPPER` aleatorio y privado en la EC2 de pagos.

`ms-analytics` usa los grupos `analytics-product-views` y
`analytics-payment-attempts`. Guarda un evento solo una vez por `eventId`,
calcula tres metricas y sugiere IDs de productos populares en la ultima
categoria vista por el usuario. Si no hay alternativas devuelve el ultimo
producto seleccionado; sin historial devuelve una lista vacia. Las selecciones
de carrito tambien se publican en product-views con accion CARRITO.

Cuando falle un consumidor, `DefaultErrorHandler` reintenta dos veces y
manda el evento a `product-views-dlt` o `payment-attempts-dlt` con su offset y
particion; el error queda en logs. El productor DLT acepta incluso bytes de
un mensaje JSON mal formado. No provocar errores deliberados en los topicos
reales durante la presentacion si no se sabe limpiar la prueba.

## 3. Despliegue por EC2 y API Gateway

La siguiente distribucion describe la ampliacion de la etapa anterior.
En el laboratorio NUEVO recrear sus funciones, no conservar ni reutilizar
las instancias antiguas. Seguir el plan ACTUAL de 5 EC2 del documento EP6.
La ampliacion agrega:

- Una EC2 para los tres brokers/ZooKeeper/Kafka UI, con memoria suficiente;
  estimar costo y cuota antes de encenderla.
- Una EC2 por nuevo microservicio: `ms-pagos:8087`, `ms-analytics:8089` y
  `kafka-admin-service:8090`. En `ms-productos` existente activar productor
  Kafka una vez probada la conectividad.
- PostgreSQL permanece en la EC2 base. `ms-pagos` y `ms-analytics` usan
  entidades y repositorios JPA con `DB_HOST` privado.

En cada EC2 nueva copiar `deploy/.env.streaming.example` a
`deploy/.env.node`, ajustar variables y ejecutar:

```bash
docker compose --env-file deploy/.env.node -f deploy/compose.streaming-service.yml config --quiet
docker compose --env-file deploy/.env.node -f deploy/compose.streaming-service.yml up -d --build
docker compose --env-file deploy/.env.node -f deploy/compose.streaming-service.yml ps
curl -f http://localhost:PUERTO/actuator/health
```

Agregar target groups del ALB interno con health check `/actuator/health`.
Crear rutas **explicitas** en la NUEVA HTTP API, con su JWT Authorizer,
VPC Link y listener del ALB. Mantener el prefijo `/api/v1` solo para
productos con ID y administracion Kafka; los paths fijos se reescriben:

| Metodo y ruta publica | Servicio | Path en EC2 | Scope |
| --- | --- | --- | --- |
| GET `/api/v1/productos/{id}` | ms-productos | `/api/v1/productos/{id}` | `pedidos.read` |
| POST `/api/v1/pagos/simular` | ms-pagos | `/pagos/simular` | `pedidos.write` |
| GET `/api/v1/analitica/resumen` | ms-analytics | `/analitica/resumen` | `pedidos.read` |
| GET `/api/v1/analitica/recomendaciones` | ms-analytics | `/analitica/recomendaciones` | `pedidos.read` |
| GET/POST/PUT/DELETE `/api/v1/kafka/{proxy+}` | kafka-admin-service | mismo path | read/write; cambios: rol Admin |

Para los paths con variables, usar `$request.path` y comprobar el valor que
recibe el servicio. El microservicio Kafka Admin verifica `Pedidos.Admin`
para cambiar topicos; el Gateway verifica el scope y cada Spring Resource
Server vuelve a validar firma, issuer, audience y vigencia del JWT. No
exponer directamente 8087/8089/8090, 9092-9094 ni ZooKeeper a Internet.

Solo despues de probar rutas con Postman, cambiar `kafkaEnabled` a `true` en
`frontend-pedidos360/src/environments/environment.prod.ts`, compilar y
publicar Angular en su EC2. MSAL adjunta el access token a `/api/*`.

## 4. Demostracion de 5 a 10 minutos

1. **0:00-1:00.** AWS: EC2 separadas, API Gateway, VPC Link, target groups
   saludables. Explicar que el navegador solo llama al Gateway.
2. **1:00-2:00.** Entra ID: usuario, scopes, rol Admin/Operador; login MSAL.
   En Postman, GET sin token 401 y con token 200.
3. **2:00-3:00.** RabbitMQ: `docker compose ps`, dos nodos, colas/DLQ,
   exchanges direct/topic. No afirmar que las colas esten replicadas por
   mostrar dos nodos.
4. **3:00-4:15.** Kafka UI: tres brokers, dos topicos de negocio con 3
   particiones/3 replicas, retencion y grupos de consumidores.
5. **4:15-6:00.** Angular: abrir detalle de producto; Kafka UI muestra key,
   partition, offset y value en `product-views`. Mostrar recomendacion.
6. **6:00-7:15.** Enviar un pago simulado; mostrar `payment-attempts`. Repetir
   tres veces con el mismo usuario para evidenciar 429 y metrica de bloqueo.
7. **7:15-8:30.** Como Admin, crear/editar/eliminar un topico de prueba con
   Postman; como Operador, demostrar 403. No eliminar los topicos del sistema.

Frase de cierre: "RabbitMQ entrega tareas que se confirman; Kafka conserva
eventos para consumidores independientes. En ambos casos el acceso REST pasa
por API Gateway y se valida otra vez en Spring Security".

## Limites que hay que decir si preguntan

- Webpay real, DynamoDB y WebSockets son extras del enunciado, no estan
  implementados. No reclamar sus puntos extra.
- La regla de bloqueo usa un token ficticio; no procesa tarjetas ni cobra.
- Kafka con tres brokers en una EC2 es una demo de replicacion entre procesos,
  no alta disponibilidad entre maquinas.
- Si Kafka no esta desplegado, la web EP3/EP4 sigue funcionando y la seccion
  Kafka permanece oculta. No presentar EP5 como desplegada hasta verificarlo.
