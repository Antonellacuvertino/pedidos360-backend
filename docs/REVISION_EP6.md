# Revision del enunciado y configuracion EP6

## Flujo evaluado

| Punto del enunciado | Implementacion |
| --- | --- |
| Pago simulado o Webpay | Simulacion en Carrito. No cobra ni usa Webpay. Solo se aceptan nueve numeros ficticios `0000 0000 0000 0001` a `0000 0000 0000 0009`. |
| Mas de tres usos de una tarjeta en 10 s | La demo usa un umbral mas estricto a pedido de la usuaria: 429 desde el tercer intento en 10 s, con alerta y bloqueo de 60 s. El criterio literal del enunciado se activa en el cuarto. Contador por HMAC de la tarjeta, no por cuenta. El numero no se registra en BD, Kafka o logs. |
| Eventos y recomendaciones | Productos publica vistas; las selecciones del Carrito Angular se publican por el endpoint de interacciones de Productos. Usan `product-views`. Analytics consume el historial y recomienda por categoria o complemento segun la ultima interaccion. |
| Microservicio libre | Auditoria registra eventos de orden desde RabbitMQ. |
| Extras | No se implementaron Webpay, DynamoDB ni WebSockets. Las metricas de Analitica se consultan por HTTP. |

El Carrito no es un microservicio Java separado en esta entrega. Si el docente
interpreta literalmente dos productores en servicios distintos, este punto
necesita confirmacion: las dos acciones existen, pero se publican desde Productos.
Cupones y envios aparecen como ejemplos en el enunciado y no forman parte de esta demo.

## Que mostrar en AWS

Revision en lectura del 25-11-2026, region Norte de Virginia (`us-east-1`).

1. **Frontend:** EC2 > Instancias > `p360-ep6-frontend`. Mostrar
   [la web HTTPS](https://100-52-32-209.sslip.io). El frontend no es una ruta
   en el Gateway: EC2 publica Angular y Angular llama a la API. La consigna
   compartida no exige una segunda API para servir HTML.
2. **Backend:** API Gateway > **Pedidos360-EP6** (`l9xrwlo874`) > Routes.
   Es HTTP API con stage `$default`. Seleccionar GET productos,
   POST pedidos/lote y GET analitica/recomendaciones. Las integraciones HTTP
   proxy usan el VPC Link `p360-ep6-link` (`AVAILABLE`) y el ALB interno.
   En EC2 > Balanceadores, mostrar las reglas y sus grupos de destino.
3. **Autorizacion:** hay 33 rutas; 32 estan protegidas. La unica sin JWT es GET
   `/api/v1/public`. Las otras requieren el autorizador Microsoft-Entra;
   lectura usa `pedidos.read` y escritura, `pedidos.write`.
   Mostrar issuer Entra v2, audience de la API, cabecera Authorization y
   CORS limitado al origen HTTPS del frontend.
4. **Roles:** Spring vuelve a validar el JWT. Las operaciones que cambian
   colas, exchanges, bindings y topicos requieren `Pedidos.Admin` en el
   servicio. El JWT authorizer usa scopes, no decide el rol. No confundir
   esto con la creacion de productos/clientes, que usa el scope de escritura.
5. **Red:** solo se permiten conexiones entrantes publicas a los puertos
   80/443 del frontend. Backend y brokers estan limitados por security groups
   referenciados; los paneles se consultan por HTTPS con autenticacion.
   Las EC2 requieren IMDSv2. El perfil compartido `LabInstanceProfile`
   usa `LabRole`, con politicas administradas del laboratorio y SSM. No es un
   rol de clientes de la aplicacion ni se verifico como minimo privilegio.
   El stage no tiene access logs configurados. No se cambiaron IAM ni los
   security groups durante la revision.

Pruebas HTTP: ruta publica 200, productos y checkout sin JWT 401. El preflight
del frontend recibe `Access-Control-Allow-Origin`; otro origen no la recibe.

**Respuesta corta:** "Hay dos direcciones, no dos Gateways. Una abre la web;
la otra recibe las llamadas HTTP con el access token. En Red/Network se ve
la llamada desde Angular a execute-api y el codigo de respuesta".
Si el profesor se refiere a un dominio personalizado en Gateway o quiere
que el HTML pase por otra entrada, confirmar la exigencia antes de cambiar
DNS, redirect de Entra o la arquitectura.
