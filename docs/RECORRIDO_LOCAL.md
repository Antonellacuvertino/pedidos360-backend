# Recorrido antes del despliegue

Actualizacion EP6: no iniciar este recorrido sobre el cluster anterior sin
revisar docs/DESPLIEGUE_MINIMO_EP5_EP6.md. Compose Kafka ahora requiere tres
ZooKeeper y un proyecto nuevo con volumenes nuevos. Los comandos de abajo
solo aplican a un entorno local nuevo; no migran datos existentes.

Este entorno es un ensayo local, no evidencia de despliegue cloud. Mantiene
JWT de Microsoft, pero usa un proxy de desarrollo en lugar de AWS API Gateway.
No cambia environment.prod.ts ni consume el Gateway del laboratorio anterior.
Pagos simulados y SMTP desactivado: no cobra dinero ni envia correos.

## Iniciar

Revisar memoria antes de iniciar todos los servicios: este ensayo combina
tres brokers Kafka y nueve JVM de aplicaciones. En un equipo de 8 GB con
otras aplicaciones abiertas puede no caber. No interpretar un contenedor
detenido por falta de memoria como fallo de autenticacion. El 23-11-2026
se compilaron ambos componentes, pero el recorrido con todos los servicios
quedo pendiente por memoria disponible. Se eligio continuar el recorrido
en el nuevo laboratorio AWS y se detuvo el servidor Angular local.
El frontend solo no permite comprar.

Con Docker Desktop encendido, desde backend-pedidos360:

```powershell
docker compose -f deploy/compose.kafka.yml up -d
mvn package -DskipTests
docker compose -f deploy/compose.recorrido-local.yml up -d
```

La red del cluster Kafka debe llamarse deploy_default; si se uso otro nombre,
definir KAFKA_DOCKER_NETWORK antes de iniciar el segundo compose.
Los JAR se montan desde target: recompilar y reiniciar el servicio despues de
modificar Java. La base PostgreSQL y los volumenes Rabbit de este ensayo son
independientes de AWS. Las claves ficticias del compose son solo locales.

Desde frontend-pedidos360:

```powershell
npm run start:local
```

Abrir http://localhost:4200. En Entra, la SPA debe tener registrada esa URI.
Entrar con una cuenta del tenant que tenga los permisos de Pedidos360.
No compartir el access token. El perfil local habilita las vistas Kafka.

## Recorrido

1. Mi cuenta: comprobar usuario, scopes pedidos.read/pedidos.write y rol.
   Ejecutar GET protegido. Sin token, la misma API debe devolver 401.
2. Productos: abrir un detalle y agregar un articulo al carrito.
3. Esperar unos segundos en Productos: revisar la seccion Para ti.
   Kafka transporta los eventos; ms-analytics calcula la recomendacion.
4. Carrito: seleccionar cliente, confirmar una compra y ver el comprobante.
   Es aceptacion asincrona, no una boleta tributaria ni un cobro Webpay.
5. Dashboard: actualizar y comprobar que el pedido se proceso.
6. RabbitMQ: http://localhost:15672, usuario pedidos360, clave recorrido-local.
   Overview muestra dos nodos. En Queues revisar consumers y estadisticas
   de entrega/ack. Ready=0 puede significar consumo inmediato, no ausencia
   de actividad. No publicar mensajes arbitrarios ni purgar colas.
7. Kafka UI: http://localhost:8088, Topics, product-views y payment-attempts.
   Revisar Messages (key, partition, offset, value), tres particiones y
   tres replicas. Hay tambien dos topicos DLT.
8. Al final, Analitica: completar clienteId y pedidoId de prueba, monto 1000,
   metodoPagoToken demo_recorrido. Registrar tres intentos en menos de diez
   segundos (esperar la respuesta de cada uno). El tercero debe dar 429.
   Reintentar una compra en Carrito: tambien debe dar 429, sin nueva orden.

La ventana se evalua en cada solicitud, no mediante un temporizador que
corre cada diez segundos. El usuario se identifica desde el JWT, no por el
clienteId del formulario. Cambiar ese campo no evita el bloqueo.
Para repetir una compra, esperar 30 minutos o usar otra cuenta autorizada.
Los eventos anteriores de pruebas pueden seguir presentes en Kafka.

## Detener solo el ensayo

```powershell
docker compose -f deploy/compose.recorrido-local.yml stop
```

Ctrl+C en la terminal de Angular. Kafka se administra por separado.
No usar down -v si se quieren conservar compras e historial local.
Falta repetir este recorrido con el dominio y Gateway del laboratorio nuevo.
