# Despliegue necesario para EP5 y EP6

## Alcance comprobado el 24-11-2026

Se leyeron EP5_DSY1107_Estudiante_encargo (1).pdf y
EP6_DSY1107_Estudiante_presentacion (1).pdf proporcionados por la estudiante.
Las referencias siguientes usan la numeracion impresa, no la portada.

- EP5, pagina 2: Angular, Spring Boot, BD cloud, JWT, RabbitMQ con DLQ,
  administracion Rabbit/Kafka y Compose con ZooKeeper, brokers y Kafka UI.
- EP5, paginas 3-6: DTO, productores/consumidores de dos topicos, manejo de
  errores, configuraciones de retencion/particiones/replicas y consumer groups.
- EP6, pagina 2: frontend y backend desplegados e integrados por API Manager,
  validacion JWT, dos nodos Rabbit, tres colas con DLQ, exchanges direct/topic,
  y dos topicos Kafka con tres particiones y tres replicas.
- EP6, pagina 3: el primer indicador exige explicar TRES nodos ZooKeeper y
  TRES brokers. La version anterior de Compose tenia solo un ZooKeeper.
- EP6, paginas 4-6: demostrar flujo real, reaccion a eventos, estructura del
  DTO, offsets, lag, grupos y retencion. La presentacion dura 5-10 minutos.

Estos PDF no especifican t3.large, una EC2 por contenedor, RDS, MSK,
Kubernetes, NAT Gateway ni Webpay. El 24-11-2026 la estudiante autorizo
compartir EC2 entre microservicios para priorizar la pauta escrita y el
despliegue funcional. Se mantienen aplicaciones y contenedores independientes.
Fraude y recomendaciones son solicitudes de la estudiante ya implementadas.
La guia adicional de Webpay debe revisarse por separado si el docente la
exige: el pago actual es SIMULADO, no una integracion Webpay terminada.

## Propuesta sin refactorizar ni quitar funciones

| EC2 | Contenido | Tipo propuesto |
| --- | --- | --- |
| 1 frontend | Angular y Caddy HTTPS, separado de backend | t3.large |
| 2 backend core | productos, clientes, pedidos, notificaciones y auditoria; cinco contenedores | t3.large |
| 3 backend support | BFF, rabbit-admin, pagos, analytics y kafka-admin; cinco contenedores | t3.large |
| 4 infraestructura | PostgreSQL y dos nodos RabbitMQ en contenedores | t3.large |
| 5 streaming | Tres ZooKeeper, tres brokers y Kafka UI en contenedores | t3.large |

Son 5 EC2 y 10 vCPU. Reemplaza las propuestas anteriores de 4/12/15 maquinas.
AWS DryRun rechazo t3.xlarge y autorizo t3.large en la cuenta nueva el 24-11-2026.
No es un minimo universal de AWS. No hay servicios Java en la EC2 frontend.
Se conserva margen de memoria en frontend para compilar Angular y en backend
para cinco JVM por host (heap maximo 512 MiB y limite de 1 GiB por contenedor).
Los tipos son una base de dimensionamiento; observar memoria/disco y
creditos CPU. Separar maquinas no garantiza que no haya lentitud.

La EC2 compartida y el host Kafka son puntos unicos de fallo. Los brokers
replican entre contenedores, no entre maquinas. Tres ZooKeeper tienen quorum
de dos, pero no sobreviven a la perdida del host compartido.
Dos nodos Rabbit tampoco prueban que las colas classic esten replicadas.

Verificar que el Readme permita estos tipos y cinco instancias. Si un tipo
no esta permitido, proponer un reemplazo con memoria suficiente, sin eliminar
servicios. No probar limites creando recursos a ciegas.

## Arquitectura y seguridad

Navegador -> Angular HTTPS en EC2.
Angular -> HTTP API Gateway con JWT Entra -> VPC Link -> ALB INTERNO ->
target group de cada microservicio. El BFF conserva resumen y endpoints demo.
Un Gateway basta: Angular no se aloja dentro de API Gateway.

No agregar RDS, MSK, EKS, ECS, Redis, NAT ni otro microservicio de
recomendaciones por defecto. ms-analytics ya realiza recomendaciones.
No sustituir Entra por Cognito ni Rabbit por Kafka.

Para este lab se puede usar subred publica con salida por Internet Gateway e
IPv4 publica, pero las aplicaciones se comunican por IP PRIVADA y las reglas
de entrada son restrictivas. Eso NO convierte la subred en privada.
La alternativa privada con NAT requiere una decision y costo adicionales.
Permitir 80/443 publicos solo en frontend/proxy HTTPS; SSH solo IP autorizada.
Puertos Spring desde ALB; support puede acceder a servicios core y core
puede acceder a pagos:8087. Usar IP privadas entre hosts y nombres Docker
dentro del mismo host. BD y mensajeria solo desde SG clientes.
El ALB acepta trafico del SG del VPC Link. Usar dos subredes/AZ para el ALB.
UI Rabbit/Kafka: tunel o HTTPS autenticado, nunca HTTP abierto a internet.
Si Caddy esta en un contenedor, localhost es ese contenedor: planificar la
red del proxy y no asumir que alcanza los puertos loopback del host.

## Orden y archivos

1. Leer Readme del laboratorio, verificar cuenta/region/cuotas/tipos, revisar
   saldo y recursos. La cuenta nueva reportada fue 767398066771: comprobarla
   en la consola, no asumir acceso. Crear solo recursos aprobados.
2. En EC2 infraestructura, usar deploy/compose.infrastructure.yml y
   deploy/.env.infrastructure.example con secretos en deploy/.env.node.
   Este Compose no arranca Java ni resetea Rabbit al reiniciar:

   docker compose -p pedidos360-base --env-file deploy/.env.node -f deploy/compose.infrastructure.yml up -d

   Comprobar rabbitmqctl cluster_status y PostgreSQL. Nunca down -v para
   apagar ni ejecutar docker compose up del archivo raiz en esta EC2:
   eso levantaria tambien los microservicios que deben ir separados.
3. Kafka: deploy/compose.kafka.yml con KAFKA_EXTERNAL_HOST=IP_PRIVADA_KAFKA.
   Usar un proyecto NUEVO en el laboratorio nuevo:

   docker compose -p pedidos360-ep6 --env-file deploy/.env.kafka -f deploy/compose.kafka.yml up -d

   IMPORTANTE: no aplicar el cambio de uno a tres ZooKeeper sobre volumenes
   del cluster anterior sin plan de migracion y copia de seguridad. No
   mezclar metadatos ZooKeeper vacios con discos Kafka antiguos. No borrar
   los volumenes anteriores ni recrear el Docker local de la estudiante.
   Verificar tres ZooKeeper operativos (lider y seguidores), tres brokers,
   kafka-init exit 0, cuatro topicos (dos negocio y dos DLT), RF3 y tres ISR.
4. En ambas EC2 backend, usar deploy/.env.backend.example como deploy/.env.node.
   Compilar una imagen con los diez JAR, ejecutar SOLO cinco contenedores por
   host. Los comandos siguientes se ejecutan uno por host, no ambos en el mismo:

   docker build -f deploy/Dockerfile.backend -t pedidos360/backend:ep6 .
   CORE: docker compose -p pedidos360-backend --env-file deploy/.env.node -f deploy/compose.backend.yml up -d ms-productos ms-clientes ms-pedidos notifications-service ms-auditoria
   SUPPORT: docker compose -p pedidos360-backend --env-file deploy/.env.node -f deploy/compose.backend.yml up -d bff-pedidos360 rabbit-admin-service ms-pagos ms-analytics kafka-admin-service

   Configurar DB_HOST, Rabbit y Kafka privados, JWT y CORS. Pepper estable y
   aleatorio solo en pagos. Definir PAGOS_URL=http://IP_SUPPORT:8087 en core.
   En support, definir URLs del BFF para productos/clientes/pedidos/notificaciones/
   auditoria con IP_CORE; rabbit-admin conserva el nombre Docker. No usar localhost para servicios de
   otra EC2. Los Compose por microservicio se conservan solo como alternativa.
   Esta imagen nueva no se ha construido en el PC por limitaciones de memoria;
   debe construirse y comprobarse en EC2. El codigo Java ya compilo con Maven.
5. Gateway/ALB: rutas base de ARQUITECTURA_MULTI_EC2.md y rutas nuevas de
   EP5_IMPLEMENTACION.md y VALIDACION_Y_LAB_NUEVO.md, sin copiar IDs antiguos.
   GET protegidos: 200; creacion asincrona de pedidos: 202; errores 401/403/429.
   Cada target group apunta a la IP privada CORE o SUPPORT segun el servicio,
   y su puerto: 8080-8087, 8089 y 8090. No usar 8088, reservado a Kafka UI.
   No exigir 200 a todos los POST. Verificar rutas dinamicas y quitar stage
   del path cuando corresponda. /actuator/health no debe exponer secretos.
6. Angular: apiBaseUrl nuevo, dominio HTTPS, URI SPA/ logout Entra y CORS
   coherentes. Activar kafkaEnabled cuando servicios/rutas ya funcionen.
   No usar start:local, proxy.local.json ni compose.recorrido-local.yml en AWS.
7. Recorrer login, roles, catalogo, compra, comprobante, stock, Rabbit,
   recomendaciones y Kafka. Hacer prueba fraude AL FINAL para no bloquear
   al usuario antes de la compra normal. SMTP real solo si se configura:
   la compra no prueba por si sola que llego un correo.

## Evidencias que faltan obtener en la nube

El Compose actualizado paso validacion estructural; NO se afirma que el
ensemble nuevo este operativo ni desplegado. Las pruebas Kafka anteriores
usaron tres brokers y UN ZooKeeper. No atribuirlas a la nueva topologia.

Guardar evidencia sin JWT/claves: quorum ZooKeeper, brokers/ISR, colas/DLQ,
eventos tipados con key/partition/offset, consumidor activo y lag, respuesta
a nuevas interacciones, reintentos/DLT controlados, administracion CRUD de
topicos y configuraciones, administracion Rabbit de colas/exchanges/bindings.
Usar recursos de prueba para operaciones destructivas, nunca topicos de negocio.
Comprobar 401 sin token, 403 Operador en administracion y 429 por fraude.
No registrar el access token ni pegarlo en documentos o capturas.

## Referencias tecnicas

- Confluent 7.5, variables ZooKeeper/ensemble:
  https://docs.confluent.io/platform/7.5/installation/docker/config-reference.html
- ZooKeeper, minimo tres servidores y quorum:
  https://zookeeper.apache.org/doc/r3.8.4/zookeeperStarted.html

Se conserva ZooKeeper porque EP6 lo pide expresamente; no se propone como
arquitectura moderna recomendada para nuevos sistemas fuera de esta evaluacion.
