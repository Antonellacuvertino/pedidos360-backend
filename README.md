# Backend Pedidos360

Backend multi-modulo en Spring Boot para Pedidos360. Incluye un BFF y tres microservicios: productos, clientes y pedidos. Todos usan Spring Security como Resource Server para validar JWT de Microsoft Entra ID.

## Modulos

| Modulo | Puerto | Descripcion |
| --- | --- | --- |
| `bff-pedidos360` | `8080` | Entrada para el frontend. Expone `/api/v1`. |
| `ms-productos` | `8081` | Catalogo de productos. |
| `ms-clientes` | `8082` | Clientes. |
| `ms-pedidos` | `8083` | Pedidos. |
| `common-security` | - | Seguridad JWT compartida. |

## Variables de entorno

```bash
AZURE_TENANT_ID=TU_TENANT_ID
AZURE_API_AUDIENCE=api://CLIENT_ID_O_APP_ID_URI
DB_HOST=HOST_RDS_POSTGRES
DB_PORT=5432
DB_NAME=pedidos360
DB_USER=postgres
DB_PASSWORD=clave
PRODUCTOS_URL=http://IP_PRIVADA_PRODUCTOS:8081
CLIENTES_URL=http://IP_PRIVADA_CLIENTES:8082
PEDIDOS_URL=http://IP_PRIVADA_PEDIDOS:8083
CORS_ALLOWED_ORIGINS=https://URL_PUBLICA_FRONTEND
```

## Endpoints principales del BFF

| Metodo | Ruta | Acceso | Uso |
| --- | --- | --- | --- |
| `GET` | `/api/v1/public` | Publico | Prueba sin token. |
| `GET` | `/api/v1` | JWT | Devuelve claims principales del token. |
| `POST` | `/api/v1` | JWT | Prueba POST protegida. |
| `GET` | `/api/v1/resumen` | JWT | Resumen dashboard. |
| `GET` | `/api/v1/productos` | JWT | Lista productos. |
| `POST` | `/api/v1/productos` | JWT + scope/rol | Crea producto. |
| `GET` | `/api/v1/clientes` | JWT | Lista clientes. |
| `POST` | `/api/v1/clientes` | JWT + scope/rol | Crea cliente. |
| `GET` | `/api/v1/pedidos` | JWT | Lista pedidos. |
| `POST` | `/api/v1/pedidos` | JWT + scope/rol | Crea pedido. |
| `GET` | `/api/v2/resumen` | JWT | Ejemplo de versionamiento. |

## Seguridad

- Valida issuer con `https://login.microsoftonline.com/<tenant-id>/v2.0`.
- Valida audience con `AZURE_API_AUDIENCE`.
- Valida firma mediante JWKS de Microsoft.
- Valida expiracion del token.
- Lee scopes desde `scp`.
- Lee roles desde `roles`.
- Permite operaciones protegidas si el token contiene `SCOPE_write-read` o roles `Pedidos.Admin` / `Pedidos.Operador`.

## Ejecutar pruebas

```bash
mvn test
```

## Empaquetar

```bash
mvn clean package
```

Los jars quedan en:

```text
bff-pedidos360/target/
ms-productos/target/
ms-clientes/target/
ms-pedidos/target/
```

