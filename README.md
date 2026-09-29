# API de Gestión de Pólizas de Arrendamiento

API REST en Spring Boot para consultar, renovar y cancelar pólizas de arrendamiento (individuales y colectivas) y gestionar sus riesgos. Cada operación que modifica estado se notifica a un mock del CORE de seguros.

## Requisitos

- JDK 17 o superior (probado con Temurin 17/21).
- Maven 3.9+ **o** IntelliJ IDEA / VS Code, que traen Maven integrado.
- No requiere base de datos externa: usa H2 en memoria.

## Cómo ejecutar

**Desde IntelliJ IDEA:** abrir la carpeta del proyecto, esperar a que Maven descargue las dependencias y ejecutar `PolizasApplication` con el botón ▶.

**Desde terminal:**

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080`. Al arrancar se cargan cuatro pólizas de ejemplo.

## Cómo ejecutar las pruebas

```bash
mvn test
```

Incluye pruebas unitarias del dominio (`PolizaTest`) y pruebas de integración de los endpoints con MockMvc (`PolizaApiIntegrationTest`).

## Seguridad

Todas las peticiones deben incluir el header:

```
x-api-key: 123456
```

Sin él, o con un valor distinto, la API responde `401`. El valor se configura en `application.yml` (`app.api-key`).

## Endpoints

| Método | Ruta | Descripción | Éxito | Errores |
|---|---|---|---|---|
| GET | `/polizas?tipo=&estado=` | Lista pólizas; ambos filtros son opcionales | 200 | 400 |
| GET | `/polizas/{id}/riesgos` | Riesgos de una póliza | 200 | 404 |
| POST | `/polizas/{id}/renovar` | Renueva: canon y prima + IPC, estado `RENOVADA` | 200 | 404, 409 |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos | 200 | 404, 409 |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo colectivas) | 201 | 400, 404, 409, 422 |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo | 200 | 404, 409 |
| POST | `/core-mock/evento` | Mock del CORE: registra el evento en logs | 202 | 400 |

Valores válidos: `tipo` = `INDIVIDUAL`, `COLECTIVA`; `estado` = `VIGENTE`, `RENOVADA`, `CANCELADA`.

### Datos de ejemplo

| id | Número | Tipo | Estado | Riesgos |
|---|---|---|---|---|
| 1 | POL-IND-001 | INDIVIDUAL | VIGENTE | 1 |
| 2 | POL-COL-001 | COLECTIVA | VIGENTE | 2 |
| 3 | POL-COL-002 | COLECTIVA | CANCELADA | 1 (cancelado) |
| 4 | POL-IND-002 | INDIVIDUAL | RENOVADA | 1 |

### Ejemplos con curl

```bash
# Listar colectivas vigentes
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=VIGENTE"

# Riesgos de la póliza 2
curl -H "x-api-key: 123456" http://localhost:8080/polizas/2/riesgos

# Renovar la póliza 1
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

# Agregar un riesgo a la colectiva 2
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"direccionInmueble":"Calle 100 # 15-20, Bogotá","arrendatario":"Camila Torres","arrendador":"Felipe Rojas"}' \
  http://localhost:8080/polizas/2/riesgos

# Cancelar el riesgo 2
curl -X POST -H "x-api-key: 123456" http://localhost:8080/riesgos/2/cancelar

# Cancelar la póliza 2 (y sus riesgos)
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/2/cancelar

# Mock del CORE
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"evento":"ACTUALIZACION","polizaId":555}' http://localhost:8080/core-mock/evento
```

El archivo `requests.http` tiene estas mismas peticiones listas para el cliente HTTP de IntelliJ o la extensión REST Client de VS Code.

### Formato de error

```json
{ "codigo": "POLIZA_CANCELADA", "mensaje": "No se puede renovar la póliza POL-COL-002 porque está cancelada", "timestamp": "2026-09-29T15:00:00Z" }
```

## Reglas de negocio implementadas

- Una póliza individual solo puede tener un riesgo. Nace con él, y la regla se valida en el único método por el que entra un riesgo (`Poliza#incluirRiesgo`).
- Agregar un riesgo exige que la póliza sea colectiva (si no, `422`) y que no esté cancelada (`409`).
- No se puede renovar una póliza cancelada (`409`).
- Renovar incrementa el canon según el IPC; la prima se recalcula como canon × meses de vigencia, y la vigencia avanza por el mismo número de meses.
- Cancelar una póliza cancela todos sus riesgos activos.
- Todas las operaciones que modifican estado notifican al CORE.

## Decisiones de diseño

- **Capas:** `controller` → `service` → `repository`, más `domain` (entidades con sus reglas), `dto`, `exception`, `client` y `config`.
- **Reglas en el dominio:** `Poliza` y `Riesgo` protegen sus invariantes. Los servicios solo orquestan (buscar, invocar la regla, notificar), y las reglas se prueban sin Spring.
- **DTOs** en lugar de exponer entidades, para evitar problemas de carga perezosa y desacoplar el contrato de la base de datos.
- **Dinero con `BigDecimal`**, redondeo `HALF_UP` a 2 decimales.
- **IPC configurable** en `application.yml` (`app.ipc`, por defecto `0.052`).
- **Notificación al CORE después del commit:** el `CoreClient` llama al mock por HTTP solo si la transacción se confirma. Si el CORE falla, se registra un `warn` y la operación no se revierte.
- **Manejo de errores centralizado** con `@RestControllerAdvice`: `404` no encontrado, `409` regla de negocio, `422` tipo de póliza inválido, `400` validación.

## Supuestos

- Una póliza `RENOVADA` puede volver a renovarse; solo se bloquea la `CANCELADA`.
- No se permite cancelar el único riesgo de una póliza individual; para retirarlo se cancela la póliza.
- En este alcance el canon se maneja a nivel de póliza.

## Consola H2

Disponible en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:polizasdb`, usuario `sa`, sin contraseña). No requiere API key.
