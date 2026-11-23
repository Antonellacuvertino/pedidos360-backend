# Prompt para trabajar en AWS desde la web

Pega este texto en Claude **sin incluir contrasenas, tokens ni archivos
`.env`**. Confirma cada cambio de costo en la consola AWS Academy.

> Estoy ampliando Pedidos360 para EP5. Lee primero el repositorio backend
> `Antonellacuvertino/pedidos360-backend` y el documento
> `docs/EP5_IMPLEMENTACION.md`. La rama principal debe contener los modulos
> `ms-pagos`, `ms-analytics`, `kafka-admin-service` y `deploy/compose.kafka.yml`.
> El frontend es `Antonellacuvertino/pedidos360-frontend`. No inventes que
> estan desplegados: comprueba el estado real.
>
> Trabaja en AWS Academy Learner Lab, region `us-east-1`, con mi sesion web
> autorizada. **Antes de crear recursos**, muestra saldo restante, cuotas EC2
> y estimacion de costo; pide mi confirmacion. Conserva el API Gateway HTTP
> `2iguro8kei`, VPC Link `2bstdy`, ALB interno y las EC2 actuales. No
> elimines ni detengas servicios EP3/EP4 sin una prueba de reemplazo.
>
> 1. Prepara una EC2 dedicada con memoria suficiente para ZooKeeper, tres
> brokers Kafka y Kafbat Kafka UI. Usa `deploy/compose.kafka.yml`. Configura
> `KAFKA_EXTERNAL_HOST` con la IP privada, verifica que `kafka-init` cree
> `payment-attempts` y `product-views`, cada uno con 3 particiones y replica
> 3, y sus DLT. Comprueba en Kafka UI tres brokers, ISR, retencion y grupos.
> No abras ZooKeeper, Kafka ni la UI a `0.0.0.0/0`; usa security groups
> privados y acceso HTTPS autenticado o tunel SSH para la UI.
> 2. Despliega `ms-pagos:8087`, `ms-analytics:8089` y
> `kafka-admin-service:8090` en EC2 separadas con
> `deploy/compose.streaming-service.yml`. Usa IP privadas para PostgreSQL y
> Kafka, un `PAYMENT_FINGERPRINT_PEPPER` aleatorio guardado solo en
> `deploy/.env.node`, nunca en Git. Activa `KAFKA_ENABLED=true` en productos
> solo cuando Kafka funcione. Verifica `actuator/health` y logs sin secretos.
> 3. Crea target group por servicio en ALB interno y rutas explicitas del
> Gateway con JWT Authorizer actual. Usa la tabla de paths y scopes del
> documento EP5. Las rutas con variables deben llegar al path exacto del
> microservicio. Prueba 401 sin JWT, 200/201 con JWT, 403 cuando el rol
> Operador intenta administrar topicos. No expongas puertos Spring al publico.
> 4. Prueba en Postman un GET de detalle de producto, un pago simulado y
> consultas de analitica; confirma los eventos con key/partition/offset en
> Kafka UI y el estado de los consumer groups. Repite cuatro veces el mismo
> token `demo_...` en diez segundos y comprueba 429. No uses tarjetas reales.
> 5. Solo despues cambia `kafkaEnabled` a true en el frontend, reconstruye y
> publica en la EC2 Angular. Comprueba que login MSAL, catalogo, carrito y
> RabbitMQ antiguos siguen funcionando. Toma capturas sin JWT ni claves.
>
> Entrégame una tabla: recurso, estado, URL privada/publica, prueba ejecutada,
> resultado y costo estimado. Si falta permiso, presupuesto o un paso no
> funciona, detente y dilo claramente; no marques como terminado lo que no
> pudiste verificar. No uses el ejemplo de un solo broker de la guia porque
> no permite tres replicas.
