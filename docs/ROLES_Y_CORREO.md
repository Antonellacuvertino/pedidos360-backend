# Usuarios, roles y correo de pedidos

## 1. Usuario personal en Microsoft Entra ID

En el tenant de la API (`e5372bf0-c5e3-4286-887c-79069f209c1f`):

1. Ir a **Entra ID > Usuarios > Nuevo usuario > Invitar usuario externo**.
2. Escribir el correo personal y enviar la invitacion. La persona debe aceptarla desde su buzon. No se crea una contrasena local para ese invitado.
3. Si el boton de invitacion no esta disponible, pedir al administrador del tenant permiso de invitacion o que realice este paso. No confundir roles del directorio con roles de la aplicacion.

## 2. Roles de la aplicacion API

1. En **Registros de aplicaciones > Pedidos360 API > Roles de aplicacion**, crear roles con estos *valores* exactos: `Pedidos.Admin` y `Pedidos.Operador`. Tipo de miembro: **Usuarios/Grupos**; habilitados.
2. En **Aplicaciones empresariales > Pedidos360 API > Usuarios y grupos > Agregar usuario/grupo**, seleccionar al usuario invitado y asignar `Pedidos.Admin` para la prueba administrativa. Otro usuario puede recibir `Pedidos.Operador`.
3. Cerrar sesion en Pedidos360, volver a entrar y abrir **Mi cuenta**. Verificar que el **access token de la API** contiene `roles: ["Pedidos.Admin"]`. Un token anterior no se actualiza solo por cambiar la asignacion.
4. Si no aparece el rol, revisar que se creo en el registro de la **API**, no solo en el frontend, y que se asigno en la aplicacion empresarial correcta.

La lectura y la compra siguen usando los scopes `pedidos.read` y `pedidos.write`. Crear o eliminar colas y exchanges requiere ademas `Pedidos.Admin` en el backend. Un token con `pedidos.write` pero sin ese rol obtiene `403` al intentar administrar RabbitMQ. `401` significa token ausente o invalido.

## 3. Correo de confirmacion

El carrito envia el correo del cliente seleccionado en cada `POST /api/v1/pedidos`. `ms-pedidos` publica `order.created`; `notifications-service` consume `notifications.queue` y genera un mensaje con referencia, fecha, IDs, cantidad y total. Es un **comprobante no tributario**, no una boleta fiscal. Si el carrito tiene varios productos, actualmente se crea un pedido y un correo por producto.

Para recibirlo en el correo personal, el cliente seleccionado en el carrito debe tener esa direccion. Si no existe, crear un cliente con `POST /api/v1/clientes` en Postman y seleccionarlo antes de confirmar la compra. No usar el correo de otra persona sin permiso.

El envio real necesita un buzon SMTP autorizado. En la EC2 de `notifications-service`, editar su `deploy/.env.node` privado y definir:

```text
SMTP_ENABLED=true
SMTP_HOST=servidor.smtp
SMTP_PORT=587
SMTP_USERNAME=cuenta_de_envio
SMTP_PASSWORD=clave_de_aplicacion_o_credencial_smtp
SMTP_FROM=remitente_autorizado
```

Para una cuenta Gmail personal:

1. En la cuenta que enviara los mensajes, activar **Verificacion en 2 pasos**.
2. Abrir [Contraseñas de aplicaciones](https://myaccount.google.com/apppasswords) y generar una para Pedidos360. Esta opcion puede no estar disponible por la politica de la cuenta. No usar la contrasena normal de Gmail.
3. En `deploy/.env.node` de la EC2 de notificaciones usar `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_USERNAME` con el correo completo de la cuenta remitente, `SMTP_PASSWORD` con la contrasena de aplicacion y `SMTP_FROM` con ese mismo correo. Definir `SMTP_ENABLED=true`.
4. Recrear solo el contenedor de notificaciones:

   ```bash
   docker compose --env-file deploy/.env.node -f deploy/compose.microservice.yml up -d --force-recreate app
   ```

5. Probar `POST /api/v1/notificaciones/prueba` con JWT valido. La respuesta `202` solo confirma que el evento entro a la cola; verificar que el correo llego al destinatario (incluido spam) y que `notifications.dlq` no recibio el evento. Consultar logs de `notifications-service` si no llega.

Con `SMTP_ENABLED=false`, **no se envia correo**: solo queda un registro de simulacion en logs. No incluir credenciales SMTP en Git, Postman, capturas ni chat. La invitacion de Entra al usuario personal no configura SMTP por si sola.

## 4. Demostracion corta

1. Mostrar el invitado en **Usuarios** y su asignacion en **Aplicaciones empresariales > Pedidos360 API**.
2. Iniciar sesion con esa cuenta, abrir **Mi cuenta** y mostrar `aud`, `scp` y `roles` sin compartir el JWT completo.
3. Crear un pedido de un producto. Mostrar `202`, el comprobante en la web, `notifications.queue` en RabbitMQ y el correo recibido **solo si SMTP real esta activado y probado**.
4. En Postman usar la peticion 12 con token de Operador: `403` esperado. Repetir con token de Admin y scope `pedidos.write`: `201` esperado. Eliminar luego `demo.presentacion` desde RabbitMQ Management si fue creada.

Referencias: [Invitar usuario externo](https://learn.microsoft.com/en-us/azure/active-directory/external-identities/b2b-quickstart-add-guest-users-portal), [Roles de aplicacion y claim `roles`](https://learn.microsoft.com/en-us/entra/identity-platform/howto-add-app-roles-in-apps), [Asignar usuarios a una aplicacion](https://learn.microsoft.com/en-us/azure/active-directory/manage-apps/assign-user-or-group-access-portal), [Contrasenas de aplicaciones de Google](https://support.google.com/accounts/answer/185833?hl=es), [SMTP de Gmail](https://support.google.com/a/answer/176600?hl=es).
