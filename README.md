# MineOps · API

Control de equipos mineros y mantenimiento por horómetro. **Este repositorio es el backend.**

Una operación minera asigna camiones, excavadoras y perforadoras a turnos de día y de noche. MineOps
impide los tres errores que el enunciado describe —asignar equipos que ya debían estar en
mantenimiento, operadores sin certificación vigente y el mismo equipo en dos turnos a la vez— y,
sobre todo, **dice por qué** cuando rechaza algo: todas las reglas incumplidas a la vez, no la
primera.

| | |
|---|---|
| **Aplicación publicada** | https://mineops-web.vercel.app |
| **API publicada** | https://mineops-api.onrender.com/api/v1 |
| **Documentación interactiva** | https://mineops-api.onrender.com/swagger-ui.html |
| **Repositorio del frontend** | https://github.com/Loberos/mineops-web |

> El razonamiento detrás del modelo de datos y la resolución de cada decisión abierta del enunciado
> están en **[DECISIONES.md](DECISIONES.md)**.

---

## Qué hace

Lo que hace interesante al sistema no son las pantallas, sino lo que ocurre debajo:

- El horómetro **solo** puede subir por un camino, y ese camino evalúa el umbral. Bloquear un equipo
  no es una tarea programada: es una consecuencia inevitable de sumar horas.
- Un rechazo devuelve **todas** las reglas incumplidas porque las reglas devuelven valores en lugar
  de lanzar excepciones. El motor no tiene forma de cortar antes.
- Cuando un equipo se bloquea a mitad de semana, las asignaciones que ya tenía **no se borran**:
  quedan marcadas para que alguien decida.
- Forzar una asignación es posible para un supervisor, imposible para el resto, y **siempre** deja
  registrado qué reglas se omitieron.

Las doce reglas del enunciado y el lugar exacto donde cada una queda garantizada están en
[DECISIONES.md §1.4](DECISIONES.md).

---

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje y framework | Java 26, Spring Boot 4.1 |
| Seguridad | Spring Security con JWT (HS256) |
| Persistencia | Spring Data JPA, PostgreSQL 17, migraciones con Flyway |
| Documentación | springdoc-openapi (Swagger UI) |
| Pruebas | JUnit 5, AssertJ, Mockito |
| Contenedor | Dockerfile multietapa (build con JDK, runtime con JRE y usuario sin privilegios) |

Arquitectura **DDD por bounded contexts**, con capas `domain` / `application` / `infrastructure` /
`interfaces`. Los contextos se comunican solo por fachadas ACL, nunca por repositorio ajeno.

---

## Cómo levantarlo en local

### Opción A — Docker Compose (base de datos + API + web)

Requiere Docker Desktop. Como el proyecto vive en dos repositorios, **clónalos como hermanos y con
estos nombres de carpeta**, que son los que espera el `docker-compose.yml`:

```bash
git clone https://github.com/Loberos/mineops-api.git mineopsapi
git clone https://github.com/Loberos/mineops-web.git mineops-web

cd mineopsapi
cp .env.example .env      # opcional: los valores por defecto funcionan tal cual
docker compose up --build
```

| Servicio | URL |
|---|---|
| Aplicación web | http://localhost:8081 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | `localhost:5432` (usuario, base y clave: `mineops`) |

La primera vez la API crea el esquema con Flyway y carga el juego de datos de ejemplo. Para detener y
borrar los datos: `docker compose down -v`.

> Si solo clonaste este repositorio, el servicio `web` no podrá construirse porque su contexto es
> `../mineops-web`. Levanta únicamente la base y la API con
> `docker compose up --build database api`.

### Opción B — Solo la API

Requiere Java 26 y un PostgreSQL accesible.

```bash
docker run --name mineops-db -e POSTGRES_DB=mineops -e POSTGRES_USER=mineops \
  -e POSTGRES_PASSWORD=mineops -p 5432:5432 -d postgres:17-alpine

./mvnw spring-boot:run
```

Escucha en `http://localhost:8080`. Variables de entorno reconocidas (todas con valor por defecto
para desarrollo):

| Variable | Por defecto | Para qué |
|---|---|---|
| `DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/mineops` | Conexión JDBC |
| `DATASOURCE_USERNAME` / `DATASOURCE_PASSWORD` | `mineops` | Credenciales |
| `JWT_SECRET` | clave de desarrollo | Firma HS256; **mínimo 32 bytes**, obligatorio cambiarla en producción |
| `JWT_EXPIRATION_HOURS` | `12` | Vigencia del token |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Orígenes permitidos, separados por coma |
| `SEED_ENABLED` | `true` | Carga los datos de ejemplo si la base está vacía |
| `PORT` | `8080` | Puerto HTTP |

---

## Credenciales de prueba

Contraseña para todas las cuentas: **`MineOps2026!`**

| Correo | Rol | Puede |
|---|---|---|
| `admin@mineops.pe` | Administrador | Todo, más catálogos y usuarios |
| `supervisor@mineops.pe` | Supervisor | Programar, **cerrar turnos** y **autorizar excepciones** |
| `planner@mineops.pe` | Planificador | Programar turnos, asignar y registrar mantenimientos |
| `viewer@mineops.pe` | Consulta | Solo lectura |

**Para ver el reto completo, entra como supervisor**: es el único rol que puede autorizar una
asignación que incumple reglas.

```bash
curl -X POST http://localhost:8080/api/v1/authentication/sign-in \
  -H 'Content-Type: application/json' \
  -d '{"email":"supervisor@mineops.pe","password":"MineOps2026!"}'
```

---

## Qué probar (los casos borde ya están cargados)

El juego de datos se construye ejecutando los mismos comandos que ejecutaría un usuario, así que todo
lo que verás es alcanzable por las reglas reales.

1. **Un equipo a punto de alcanzar su mantenimiento.** `CAM-001` está a 8 horas de su umbral de 250 h.
2. **Un turno que al cerrarse dispara el bloqueo.** El **turno de día de hoy** está abierto a
   propósito. Ciérralo (`POST /shifts/{id}/close`): `CAM-001` y `EXC-002` cruzan su umbral con las
   12 h del turno y quedan **BLOQUEADOS** al instante.
3. **Un operador con certificación vencida.** **María Huamán** tiene su certificación de camión
   vencida hace 10 días. Asígnala a un camión: el sistema responde **422** explicando por qué.
4. **Todas las reglas a la vez (regla 11).** Pide una asignación con un operador y un equipo que ya
   estén asignados en ese mismo turno, siendo además el equipo uno bloqueado: la respuesta trae
   **cuatro** violaciones simultáneas, cada una indicando si puede autorizarse o no.
5. **Una certificación que vence a mitad de turno.** **Carlos Mamani** tiene su certificación de
   camión venciendo en 3 días y ya está asignado al **turno de noche de ese día**, que termina a la
   mañana siguiente: la asignación está marcada como forzada, con el código
   `CERTIFICATION_EXPIRES_DURING_SHIFT` en su autorización.
6. **Un equipo bloqueado con turnos ya programados.** `PER-001` cruzó su umbral al cerrarse el turno
   de ayer. Sus asignaciones de los días siguientes **no se borraron**: están en `AT_RISK`
   (`GET /assignments/at-risk`).
7. **Un mantenimiento hecho tarde.** `CAM-002` fue atendido 10 h después de su umbral. En su
   historial verás el desfase registrado y que el siguiente umbral quedó en 500 h (no en 510).
8. **Proyección a 7 días.** `GET /maintenance-projection?horizonDays=7` dice en qué turno exacto
   cruzará su umbral cada equipo, recorriendo el calendario en orden.
9. **Cierre con horas distintas a lo planificado.** Cierra un turno cambiando las horas de una
   asignación a 5 (contra 12 planificadas) y sin justificación: el cierre se rechaza pidiendo el
   motivo por escrito.
10. **Un mantenimiento hecho antes de tiempo.** `CAM-004` fue atendido a las 120 h de un umbral de
    250. Su siguiente umbral quedó en **370**, no en 250: es el caso contrario al de `CAM-002`, y
    juntos muestran por qué el ciclo no puede calcularse siempre igual.
11. **Un turno cerrado con más horas de las planificadas.** El de anteayer se programó a 8 h y se
    trabajaron 10. Se aceptó porque venía justificado; sin el motivo, el cierre se habría rechazado.
12. **Una certificación que vence hoy mismo.** **Pedro Condori** en perforadora. Los rangos son
    cerrados en ambos extremos, así que hoy todavía puede operar y mañana no.
13. **Máquinas detenidas por decisión de una persona.** `EXC-003` está en el taller
    (`IN_MAINTENANCE`) y `PER-003` dada de baja (`OUT_OF_SERVICE`). Ninguna puede asignarse, y el
    motivo que devuelve la API es distinto del bloqueo por umbral.
14. **Una asignación cancelada.** En el turno de dentro de cinco días. La fila sobrevive como
    historia y deja libres al operador y a la máquina para un reemplazo.

---

## Pruebas

```bash
./mvnw test
```

63 pruebas: 55 unitarias sobre las reglas de negocio (sin base de datos) y 8 de integración que
arrancan la aplicación completa. No requieren Docker.

| Clase | Qué cubre |
|---|---|
| `MaintenanceCycleTest` | Cálculo del siguiente umbral: mantenimiento tardío, adelantado, atraso mayor a un ciclo, y que el desfase **no** se acumula en tres ciclos seguidos |
| `EquipmentTest` | Bloqueo automático al cruzar el umbral, liberación por mantenimiento, horómetro que no retrocede |
| `ShiftTest` | Reglas 6 y 7, marcado en riesgo, cierre y cancelación |
| `AssignmentRuleEvaluatorTest` | El motor completo con sus siete reglas reales: acumulación de cuatro violaciones a la vez, qué se puede autorizar y qué no, y el par de casos de la certificación que vence a mitad de turno |
| `OperatorTest` | Vigencia de certificaciones y renovación sin duplicar |
| `DemoScenarioIntegrationTest` | Arranca el contexto completo contra una base en memoria y verifica el escenario de ejemplo de punta a punta: el cierre de turno que bloquea la perforadora, las asignaciones que quedaron en riesgo por ello, la traza de auditoría de las excepciones autorizadas y la proyección |

La prueba de integración vale doble: como el seed ejecuta **los mismos comandos que un usuario**, si
las reglas no se comportaran como se espera el escenario no se habría podido construir siquiera. De
paso valida el mapeo JPA, todas las consultas y el cableado de Spring.

---

## Estructura

```
mineopsapi/
├── docker-compose.yml          PostgreSQL + API + web (el web se construye desde ../mineops-web)
├── Dockerfile
├── .env.example
├── DECISIONES.md               Modelo de datos, decisiones abiertas, uso de IA
└── src/main/java/com/mineops/mineopsapi/
    ├── iam/                    Usuarios, roles, JWT
    ├── assets/                 Equipos, horómetro, mantenimiento
    ├── workforce/              Operadores y certificaciones
    ├── operations/             Turnos, asignaciones, motor de reglas, proyección
    └── shared/                 Kernel compartido y carga de datos de ejemplo
```

Cada contexto repite las mismas capas:

```
<contexto>/
├── domain/          model/{aggregates,entities,valueobjects,commands,queries}, services/
├── application/     internal/{commandservices,queryservices,eventhandlers,outboundservices}
├── infrastructure/  persistence/, y lo que dependa de un proveedor concreto
└── interfaces/      rest/{controllers,resources,transform}, acl/
```

El esquema son diez tablas versionadas con Flyway en `src/main/resources/db/migration/`. Hibernate
corre con `ddl-auto: validate`: si el mapeo se desalinea del SQL, la aplicación **no arranca**.

---

## API

Documentación interactiva en `/swagger-ui.html`. Los endpoints que concentran el reto:

| Método | Ruta | Qué hace |
|---|---|---|
| `POST` | `/api/v1/authentication/sign-in` | Autentica y devuelve el token |
| `POST` | `/api/v1/shifts/{id}/assignments/preview` | Evalúa una asignación **sin escribir**: devuelve todas las reglas que incumpliría |
| `POST` | `/api/v1/shifts/{id}/assignments` | Asigna. Con `force: true` pide autorización de supervisor |
| `POST` | `/api/v1/shifts/{id}/close` | Cierra el turno y suma las horas a los horómetros |
| `GET` | `/api/v1/maintenance-projection?horizonDays=7` | Proyección a 7 días |
| `GET` | `/api/v1/assignments/at-risk` | Asignaciones comprometidas por un cambio posterior |
| `POST` | `/api/v1/equipment/{id}/maintenance-records` | Registra mantenimiento y libera el equipo |

Un rechazo por reglas de negocio devuelve **422** con la lista completa:

```json
{
  "timestamp": "2026-08-14T21:40:11",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "The assignment breaks 3 business rule(s)",
  "path": "/api/v1/shifts/4/assignments",
  "violations": [
    { "code": "OPERATOR_ALREADY_ASSIGNED", "message": "…", "severity": "BLOCKING", "overridable": false },
    { "code": "EQUIPMENT_NOT_AVAILABLE",   "message": "…", "severity": "BLOCKING", "overridable": true  },
    { "code": "OPERATOR_CERTIFICATION_EXPIRED", "message": "…", "severity": "BLOCKING", "overridable": true }
  ]
}
```

Otros códigos: **409** cuando otro usuario se adelantó (índice único o bloqueo optimista), **403**
cuando el rol no alcanza para autorizar una excepción, **401** con token ausente o vencido.

---

## Despliegue

**API y base de datos en Render**

El despliegue está descrito como código en [`render.yaml`](render.yaml), así que no hay que
configurar nada a mano salvo el secreto:

1. En Render, *New → Blueprint* y elegir el repositorio `Loberos/mineops-api`, rama `main`.
2. Render lee `render.yaml` y propone dos recursos: el servicio web **mineops-api**, construido desde
   el `Dockerfile`, y la base **mineops-db**. Quedan enlazados solos: host, puerto, nombre de base,
   usuario y contraseña se inyectan desde la base, sin copiar credenciales a ningún lado.
3. La única variable que pide valor es **`JWT_SECRET`**, declarada con `sync: false` justamente para
   que no viva en un repositorio público. Una clave de al menos 32 bytes:

   ```bash
   openssl rand -base64 48
   ```

4. *Apply*. Flyway crea el esquema y el seed carga los datos en el primer arranque.

Lo que el blueprint ya deja resuelto:

| Ajuste | Valor | Por qué |
|---|---|---|
| `healthCheckPath` | `/actuator/health` | El despliegue no se da por bueno hasta que Spring esté arriba |
| `CORS_ALLOWED_ORIGINS` | la URL del frontend | La API compara orígenes exactos, sin comodines ni barra final |
| `SEED_ENABLED` | `true` | Carga los casos borde descritos más arriba |
| `TZ` | `America/Lima` | El seed se construye alrededor de "hoy"; con el contenedor en UTC ese "hoy" cambia de día a las 19:00 hora de Perú |

El puerto no se fija: Render inyecta `PORT` y la aplicación lo respeta (`server.port: ${PORT:8080}`).

> **Sobre el plan gratuito.** El servicio se duerme tras 15 minutos sin tráfico y tarda cerca de un
> minuto en despertar, así que la primera visita después de un rato es lenta. Un monitor externo
> —UptimeRobot, cron-job.org— llamando a `/actuator/health` cada 10 minutos lo mantiene despierto sin
> costo. La base gratuita, además, caduca a los 30 días de creada.

Comprobación de que quedó bien, sin necesidad de abrir la interfaz:

```bash
curl https://<API>/actuator/health                 # {"status":"UP"}
curl -X POST https://<API>/api/v1/authentication/sign-in \
  -H 'Content-Type: application/json' \
  -d '{"email":"supervisor@mineops.pe","password":"MineOps2026!"}'
```

El frontend se despliega aparte, desde su propio repositorio: ver
[mineops-web](https://github.com/Loberos/mineops-web).

---

## Nota sobre el idioma

Los identificadores del código —clases, métodos, variables, nombres de tabla— están en inglés, que es
la convención del ecosistema y lo que hace que el código se lea igual que las bibliotecas con las que
convive. Todo lo demás está en castellano: comentarios, documentación, mensajes de la interfaz, los
textos que devuelve la API y los registros del log. Quien opera la mina no debería tener que leer
inglés para entender por qué el sistema rechazó una asignación.
