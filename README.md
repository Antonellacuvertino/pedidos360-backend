# Pedidos360 · Backend EP6

Backend de una tienda de prueba. En EP6 incorporamos un pago simulado con control de intentos por tarjeta y usamos Kafka para registrar actividad y recomendar productos. Conservamos RabbitMQ para procesar los pedidos.

## Entrega

- [Informe EP6](docs/INFORME_EP6.md): arquitectura, decisiones, seguridad, pruebas y alcance.
- [Guía de presentación](docs/PRESENTACION_EP6.md): secuencia para mostrar AWS, Kafka UI, RabbitMQ y Postman.
- [Repositorio frontend](https://github.com/Antonellacuvertino/pedidos360-frontend).

| Servicio | Dirección |
| --- | --- |
| Aplicación | [Pedidos360](https://100-52-32-209.sslip.io) |
| API Gateway | [Prueba pública](https://l9xrwlo874.execute-api.us-east-1.amazonaws.com/api/v1/public) |
| Kafka UI | [Panel de streaming](https://kafka.100-52-32-209.sslip.io) |
| RabbitMQ | [Panel de mensajería](https://rabbit.100-52-32-209.sslip.io) |

## Código y verificación

- `ms-pagos` controla pagos simulados; `ms-analytics` consume eventos y propone complementos.
- `ms-pedidos`, `ms-productos`, `ms-clientes`, `notifications-service` y `ms-auditoria` manejan el flujo de la tienda.
- `bff-pedidos360` entrega el resumen. `kafka-admin-service` y `rabbit-admin-service` administran la mensajería.
- [deploy/](deploy/) contiene los Compose y scripts. [postman/](postman/) contiene la colección EP6 y su entorno, sin tokens.

Con Java 21 y Maven:

```bash
mvn test
mvn -DskipTests package
```

Para el ensayo se usa la URL de AWS. Los archivos `.env`, contraseñas y tokens se mantienen fuera de Git.
La [plantilla](.env.example) incluye los enlaces y la configuración pública.
En el equipo del despliegue, `deploy/preparar-env-ep6.ps1` puede generar el
`.env` privado con las claves vigentes del laboratorio.
