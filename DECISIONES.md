# Decisiones

Este documento explica **cómo** modelé el problema y **por qué**, cómo resolví cada una de las
decisiones abiertas del enunciado, qué dejé fuera y qué haría con más tiempo.

> MineOps se entrega en dos repositorios: **[mineops-api](https://github.com/Loberos/mineops-api)**
> (backend, este repositorio) y **[mineops-web](https://github.com/Loberos/mineops-web)** (frontend).
> Este documento es el mismo en ambos, para que se pueda leer completo sin saltar de repositorio. Las
> rutas de código que se citan pertenecen al repositorio donde vive esa capa; casi todas las reglas de
> negocio que se explican aquí viven en este repositorio.

---

## 1. Modelado de datos

### 1.1 Por qué cuatro contextos y no un solo módulo

El enunciado tiene reglas que se cruzan, y esa es justamente la razón para separar en lugar de
mezclar: si todo vive en el mismo paquete, la regla "no asignar un equipo bloqueado" termina siendo
un `if` dentro de un servicio de turnos que lee la tabla de equipos, y nadie puede garantizar que no
haya otro camino que suba el horómetro sin evaluar el bloqueo.

| Contexto | Qué posee | Reglas del enunciado |
|---|---|---|
| `iam` | Usuarios, roles, autenticación JWT | Autorización de excepciones |
| `assets` | Tipos de equipo, equipos, horómetro, historial de mantenimiento | 1, 2, 3 |
| `workforce` | Operadores y certificaciones | 4 |
| `operations` | Turnos, asignaciones, motor de reglas, cierre, proyección | 5 a 12 |

Los contextos **no se leen entre sí por repositorio**. Cada uno publica una fachada en
`interfaces/acl/` y los demás solo pueden usar eso:

- `EquipmentContextFacade` expone `EquipmentSnapshot` (inmutable) y un único método de escritura,
  `registerUsage`. Esa es la razón por la que **es imposible subir el horómetro sin que se evalúe el
  umbral**: no existe otro camino.
- `OperatorContextFacade` es solo de lectura. Ningún otro contexto tiene motivo para cambiar quién
  está certificado.
- `UserContextFacade` resuelve la identidad del supervisor que autoriza una excepción.

### 1.2 Las decisiones de frontera que importan

**El mantenimiento vive con el equipo, no en un contexto aparte.** Registrar un mantenimiento cambia
el estado del equipo, su horómetro y su próximo umbral, y además escribe una fila de historial. Eso
tiene que ser una sola transacción; separarlo en dos contextos habría significado consistencia
eventual sobre un invariante que no la admite (un equipo liberado sin constancia de por qué).

**Las certificaciones viven dentro del operador.** Una certificación no tiene sentido fuera de la
persona que la tiene. Además, el invariante "como máximo una certificación por tipo de equipo" solo
se puede hacer cumplir desde el agregado: si hubiera dos filas vivas para el mismo tipo, la pregunta
"¿está certificado?" tendría dos respuestas justo cuando importa. Renovar **reemplaza la vigencia**,
no agrega una fila (`uk_certifications_operator_type`).

**Las asignaciones viven dentro del turno.** Esta es la decisión de modelado más importante del
reto. Las reglas 6 y 7 ("un operador no puede tener dos asignaciones en el mismo turno", "un equipo
no puede estar asignado dos veces en el mismo turno") son afirmaciones **sobre el turno completo**,
no sobre una asignación aislada. Al hacer del turno el agregado:

- la verificación y la escritura ocurren sobre un mismo objeto cargado en una misma transacción;
- el `@Version` del turno hace que dos supervisores que editan la misma dotación choquen en lugar de
  pisarse (ver §4);
- cerrar el turno es una operación del turno, que recorre sus propias asignaciones.

**Las referencias entre contextos son por identificador, con snapshot del nombre.** Una asignación
guarda `operator_id` y también `operator_name`; un registro de mantenimiento guarda `equipment_id` y
`equipment_code`. No es duplicación por descuido: **una traza de auditoría debe describir lo que era
cierto cuando ocurrió el hecho**. Si un equipo se renumera o un operador deja la empresa, el
historial sigue siendo legible. Como efecto secundario, listar una dotación no requiere joins contra
otros contextos.

### 1.3 Esquema

Diez tablas, versionadas con Flyway (`src/main/resources/db/migration/`). Hibernate corre con
`ddl-auto: validate`: el esquema lo define el SQL, y si el mapeo se desalinea la aplicación **no
arranca**, en lugar de fallar en la primera consulta.

```
users ──< user_roles >── roles

equipment_types ──< equipment ──< maintenance_records
       │
       └──< certifications >── operators
                                   │
shifts ──< assignments >───────────┘
   └──────────────────< assignments >── equipment
```

Decisiones concretas de esquema que vale la pena señalar:

- `equipment.version` y `shifts.version` para bloqueo optimista.
- Índices únicos **parciales** en `assignments` (§4).
- `CHECK` sobre los enums en base de datos: el estado de un equipo no puede tomar un valor que el
  dominio no conoce, aunque alguien escriba por fuera de la aplicación.
- `CHECK ck_assignments_authorization_complete`: si hay un usuario que autorizó, tiene que haber
  motivo y fecha. Una excepción a medio registrar no es mejor que ninguna.
- `maintenance_records` guarda `threshold_hours`, `overrun_hours` y `next_threshold_hours`. El
  desfase queda **visible** en cada fila en vez de escondido dentro del siguiente umbral.
- Horómetros y horas en `NUMERIC(12,2)`, nunca en punto flotante. Son cantidades que se acumulan y se
  comparan contra un umbral; un error de redondeo se convierte en un equipo que opera de más.

### 1.4 Dónde vive cada regla del enunciado

| # | Regla | Dónde está garantizada |
|---|---|---|
| 1 | Código, tipo y horómetro | `Equipment`, `EquipmentType` |
| 2 | Al alcanzar el intervalo, BLOQUEADO | `Equipment.evaluateThreshold()`, privado e invocado por toda mutación del horómetro |
| 3 | Mantenimiento libera y deja historial | `MaintenanceCommandServiceImpl`, una transacción |
| 4 | Certificación vigente por tipo | `Operator`, `ValidityPeriod` |
| 5 | Turno y asignación | `Shift`, `Assignment` |
| 6 | Un operador, una asignación por turno | `Shift.assign()` + índice único parcial |
| 7 | Un equipo, una asignación por turno | `Shift.assign()` + índice único parcial |
| 8 | No asignar equipo bloqueado | `EquipmentMustBeAvailableRule` |
| 9 | No asignar sin certificación vigente | `OperatorMustBeCertifiedRule` |
| 10 | Al cerrar, sumar horas al horómetro | `ShiftCommandServiceImpl.handle(CloseShiftCommand)` |
| 11 | Mostrar **todas** las reglas incumplidas | `AssignmentRuleEvaluator` (§2) |
| 12 | Proyección a 7 días | `MaintenanceProjectionQueryServiceImpl` (§3) |

---

## 2. El motor de reglas (regla 11)

La regla 11 pide que un rechazo muestre **todas** las razones, no solo la primera. Eso es fácil de
prometer y fácil de romper: basta que alguien agregue una validación con `throw` en medio del flujo
para que el sistema vuelva a abortar en el primer fallo.

Por eso las reglas **no lanzan excepciones, devuelven valores**:

```java
public interface AssignmentRule {
    Optional<BusinessRuleViolation> evaluate(AssignmentContext context);
}
```

`AssignmentRuleEvaluator` recibe todas las implementaciones por inyección, las ejecuta y acumula.
**No tiene forma de cortar antes**: la propiedad se cumple por construcción, no por acordarse.

Cada violación es un valor con cuatro datos: `code` (estable, la interfaz lo usa para agrupar),
`message` (listo para mostrar), `severity` y `overridable`.

| Código | Severidad | ¿Autorizable? | Por qué |
|---|---|---|---|
| `SHIFT_NOT_OPEN` | BLOQUEA | No | El turno ya fue liquidado; sus horas ya se contaron |
| `OPERATOR_INACTIVE` | BLOQUEA | No | La persona no está en la operación |
| `OPERATOR_ALREADY_ASSIGNED` | BLOQUEA | **No** | Nadie puede estar en dos cabinas a la vez |
| `EQUIPMENT_ALREADY_ASSIGNED` | BLOQUEA | **No** | Dos personas no manejan el mismo camión |
| `EQUIPMENT_NOT_AVAILABLE` | BLOQUEA | Sí | Es una decisión operativa real (§3.2) |
| `OPERATOR_NOT_CERTIFIED` | BLOQUEA | Sí | Es una decisión de riesgo, no un imposible |
| `OPERATOR_CERTIFICATION_EXPIRED` | BLOQUEA | Sí | Ídem |
| `CERTIFICATION_EXPIRES_DURING_SHIFT` | BLOQUEA | Sí | Ídem (§3.5) |
| `EQUIPMENT_WILL_REACH_THRESHOLD` | ADVIERTE | — | No impide nada; avisa para reservar taller |

La distinción entre "bloquea y se puede autorizar" y "bloquea y no lo puede autorizar nadie" es
deliberada. Un supervisor puede aceptar un riesgo; **no puede volver verdadera una dotación
físicamente imposible**. Confundir ambas cosas habría sido convertir el permiso de supervisor en un
`--force` que apaga el sistema entero.

El endpoint `POST /shifts/{id}/assignments/preview` corre **el mismo motor** sin escribir nada, así
que lo que el planificador ve mientras elige es exactamente lo que obtendría al enviar. Compartir el
motor es lo que impide que la vista previa se desincronice de la decisión real.

---

## 3. Las decisiones abiertas del enunciado

### 3.1 Un equipo se bloquea a mitad de semana y ya tenía turnos programados

**Qué hice:** ni se borran ni se ignoran. Cada asignación futura de ese equipo pasa a **`AT_RISK`**
con el motivo adjunto, y aparece en una lista de trabajo (`GET /assignments/at-risk`, y el panel de
inicio). Un humano decide si reasigna o cancela.

**Por qué:** borrarlas tira a la basura el trabajo del planificador y, peor, borra la evidencia de
que existieron; dejarlas intactas permite que la programación prometa máquinas que no pueden operar.
Marcarlas conserva ambas cosas: el dato y la alarma. **El sistema levanta la mano; la persona
decide.**

**Cómo está implementado:** `Equipment` publica un evento de dominio `EquipmentBlockedEvent` al
cruzar el umbral; `EquipmentLifecycleEventHandler`, en el contexto `operations`, escucha y marca las
asignaciones. El handler corre con `Propagation.MANDATORY`, dentro de la misma transacción que
bloqueó el equipo: **el bloqueo y las marcas aterrizan juntos o no aterrizan**. El contexto `assets`
no sabe que existen los turnos.

Simétricamente, `EquipmentReleasedEvent` levanta las marcas cuando el equipo vuelve del taller,
porque a esa altura ya no hay nada que decidir. Las asignaciones que un humano ya canceló **no
resucitan**.

### 3.2 ¿Se puede forzar una asignación con autorización de supervisor?

**Sí, con tres condiciones.**

1. Solo para violaciones marcadas `overridable`. Las imposibles no se pueden forzar ni siendo
   administrador.
2. Solo roles `SUPERVISOR` o `ADMIN`. Se verifica en el servidor contra el usuario del token, no
   contra lo que diga el cliente.
3. **El motivo es obligatorio.** Sin texto, no hay asignación.

**Cómo queda registrada:** en un value object embebido `SupervisorAuthorization` con quién autorizó,
su nombre, cuándo, el motivo y —la parte que se olvida— **los códigos exactos de las reglas que se
omitieron**, congelados en ese momento. Para cuando alguien lea ese registro, es muy posible que la
máquina ya haya sido atendida y la certificación renovada; lo que se está preguntando es **qué se
sabía al momento de decidir**. Además queda un `LOGGER.warn` con todos los datos.

**Por qué permitirlo:** una máquina treinta horas pasada de su umbral un viernes por la noche, con
el taller abriendo el lunes, es una decisión real que alguien tiene que tomar. Prohibirla no hace que
desaparezca: hace que se tome en una hoja de cálculo, fuera del sistema. Lo que el sistema exige es
que **la decisión esté firmada**.

### 3.3 El mantenimiento se hizo 30 horas después del umbral

**Qué hice:** el siguiente umbral se ancla al **umbral planificado**, no al horómetro real.

```
umbral 250, mantenimiento a las 280  →  siguiente umbral 500   (no 530)
```

**Por qué:** contar desde el horómetro real hace que el atraso se acumule: 250 → 530 → 810, y a los
pocos ciclos la máquina opera sistemáticamente fuera de lo que el fabricante previó. Anclar al umbral
**absorbe** el desfase en lugar de arrastrarlo.

Dos matices que el enunciado no pide pero que la regla ingenua rompe:

- **Mantenimiento adelantado** (a las 200 con umbral 250): ahí sí se cuenta desde el horómetro real
  (200 + 250 = 450). Anclar al umbral habría castigado a quien atiende la máquina antes de tiempo
  dándole un ciclo más corto.
- **Atraso mayor a un ciclo completo** (a las 620 con umbral 250): 500 ya quedó atrás, así que el
  cálculo avanza hasta el primer umbral que sigue estando adelante (750). Si no, la máquina saldría
  del taller ya vencida.

Está todo en el value object `MaintenanceCycle`, que es una función pura y tiene sus propias pruebas
(`MaintenanceCycleTest`, incluido el caso de tres mantenimientos tardíos seguidos que verifica que el
desfase **no** se acumula). El desfase de cada mantenimiento queda registrado en `overrun_hours`,
visible en el historial: se absorbe, pero no se oculta.

### 3.4 El turno se cerró con más o menos horas de las planificadas

**Qué hice:** manda lo que se trabajó. Se registran las horas reales y **esas** son las que se suman
al horómetro. Un turno de 8 h que duró 10 puso 10 horas sobre la máquina, y fingir otra cosa
corrompería el cronograma de mantenimiento del que depende todo lo demás.

Lo que el sistema sí exige es que **una desviación material se explique por escrito**. Por encima de
una tolerancia configurable (`mineops.operations.shift-closure-tolerance`, 20 % por defecto) el
cierre se rechaza si no hay justificación. Por debajo, se considera variación normal y no molesta a
nadie.

Detalles: las horas se validan entre 0 y 24; una asignación que no se informa se liquida con las
horas planificadas; y **todos** los problemas del cierre se acumulan y se devuelven juntos, igual que
en las asignaciones, en lugar de obligar a corregir de a uno.

### 3.5 Una certificación vence a mitad de un turno ya programado

**Qué hice:** la certificación debe cubrir el turno **completo**, no solo su inicio. Media guardia
certificada no es una guardia certificada.

Para eso el turno es un intervalo real, no una fecha: la jornada `DAY` empieza 07:00 y `NIGHT` 19:00,
y el fin se deriva de las horas planificadas. Un turno de noche de 12 h **termina al día siguiente**,
así que una certificación que vence ese día lo cubre a medias.

El caso tiene **su propio código** (`CERTIFICATION_EXPIRES_DURING_SHIFT`) y su propio mensaje, en vez
de mezclarse con "certificación vencida": son situaciones operativas distintas y el supervisor
necesita distinguirlas. Es autorizable —con relevo confirmado a medianoche puede ser razonable— y
queda firmada como cualquier otra excepción.

Está probado en `AssignmentRuleEvaluatorTest`, con el par de casos que lo hace evidente: el mismo
vencimiento **rechaza** el turno de noche y **acepta** el de día.

### 3.6 Dos supervisores asignan el mismo equipo al mismo turno a la vez

**Tres capas, a propósito**, porque cada una falla de forma distinta:

1. **Frontera del agregado.** El turno se carga y se escribe completo, así que la verificación y la
   escritura ocurren sobre una misma foto consistente.
2. **Bloqueo optimista.** `shifts.version` (y `equipment.version` para el horómetro). El segundo que
   escribe pierde y recibe **409** con un mensaje que le pide refrescar, en vez de sobrescribir en
   silencio. Sin esto, dos cierres simultáneos sobre el mismo equipo perderían uno de los dos
   incrementos del horómetro.
3. **Índices únicos parciales en PostgreSQL.** La garantía que sobrevive incluso a un bug en las
   capas de arriba:

```sql
CREATE UNIQUE INDEX uk_assignments_shift_equipment
    ON assignments (shift_id, equipment_id)
    WHERE status <> 'CANCELLED';
```

Son **parciales** por una razón concreta: una asignación cancelada libera al operador y a la máquina
para que entre un reemplazo, pero la fila cancelada sobrevive como historia. Un índice único total
habría obligado a borrar filas para poder reprogramar.

`DataIntegrityViolationException` y `ObjectOptimisticLockingFailureException` se traducen a **409
Conflict** en el `GlobalExceptionHandler`. Nunca a un 500.

---

## 4. Proyección a 7 días (regla 12)

El enunciado subraya que esto no se resuelve mirando el estado actual, y tiene razón. Un camión a 40
horas de su umbral **se ve perfectamente sano hoy** y tiene tres turnos de 12 h esta semana.

La implementación **recorre el calendario**: toma los turnos programados dentro del horizonte en
orden cronológico y acumula sus horas planificadas sobre una copia del horómetro de cada equipo. El
momento en que ese acumulado cruza el umbral es el turno donde la máquina se va a detener.

Sumar las horas de la semana habría dicho **si** ocurre, pero no **cuándo**, y el cuándo es
precisamente lo que se necesita para reservar taller. Por eso la respuesta incluye fecha, jornada, id
del turno y el horómetro proyectado en ese punto.

Los equipos ya detenidos aparecen primero aunque no tengan turnos: no van a llegar a mantenimiento,
ya llegaron.

---

## 5. Decisiones de plataforma

**Fechas.** Una certificación vigente "hasta el 20" incluye el 20; los rangos son cerrados en ambos
extremos, que es como los lee un operador. La lógica está en `ValidityPeriod`, no repartida entre
comparaciones sueltas.

**Roles.** Cuatro: `VIEWER` (consulta), `PLANNER` (arma la programación), `SUPERVISOR` (además cierra
turnos y autoriza excepciones), `ADMIN` (además administra catálogos y usuarios). Se declaran con
`@PreAuthorize` junto a cada endpoint, no en una tabla lejana.

**Errores.** Un solo envoltorio (`ApiErrorResource`) para toda la API. Los detalles internos se
registran en el log y **nunca** se envían al cliente. Un rechazo por reglas de negocio es **422**,
que es semánticamente distinto de un 400 por request malformado.

**Un solo desbloqueo.** El endpoint de cambio de estado **rechaza** liberar un equipo bloqueado. La
única salida de `BLOCKED` es registrar el mantenimiento, que es lo que deja el historial. Permitir el
atajo habría vuelto opcional la regla 3.

**Confirmación antes de destruir.** Toda acción que cancela, desactiva o saca algo de operación pasa
por un diálogo, pero no todos son el mismo diálogo, y la diferencia importa. Donde la API exige un
motivo —cancelar un turno, cancelar una asignación— el diálogo pide ese texto y escribirlo **es** la
confirmación: encadenar un «¿seguro?» antes de un formulario solo entrena a la gente a hacer clic sin
leer. Donde no hay motivo que registrar —desactivar un operador, mandar un equipo al taller— basta un
`ConfirmService` compartido, que centraliza el tono: el botón que destruye siempre es rojo y dice qué
destruye («Sí, enviar a taller», nunca «Aceptar»), y el foco inicial cae en el botón que *no* hace
nada, para que un Enter distraído no cueste un turno. Antes de esto «Cancelar turno» era un clic
directo que además inventaba el motivo por el usuario (`"Turno cancelado desde el panel de
operaciones"`): exactamente el registro que no sirve cuando alguien audita por qué se cayó una
dotación entera. Reactivar a un operador, en cambio, no pregunta nada: devolver algo que ya existía
no es destruir.

**Paginación.** Es del lado del servidor. Los seis listados que crecen con el uso —flota, turnos,
dotación, historial de mantenimiento de la flota y de una máquina, usuarios— aceptan `page` y `size`
y responden un envoltorio único, `PagedResource`, con el tramo pedido y el total. El tamaño tiene un
tope de 100 (`PageCriteria.MAX_SIZE`) para que un `?size=100000` no convierta un listado en una
descarga de la tabla entera, y los parámetros se normalizan en vez de rechazarse: una página
negativa llega de un parámetro escrito a mano, y ahí vale más devolver la primera que un error.

No se pagina todo. Los catálogos fijos (roles, familias de equipo) y las vistas que el dominio ya
acota (asignaciones en riesgo, certificaciones por vencer, la proyección de siete días) siguen
viajando enteras: trocear una lista de tres elementos añade ruido al contrato sin resolver nada. El
navegador conserva `paginate()` para esos casos, y ambas paginaciones cumplen la misma interfaz, así
que ni las plantillas ni `<app-paginator>` distinguen una de otra.

El vocabulario de paginación es propio (`PageCriteria`, `PagedResult`) y no el `Pageable` de Spring
Data. No es purismo: mantiene las interfaces de los servicios de consulta libres de la
infraestructura de persistencia, y evita serializar el `Page` de Spring, cuya forma interna el
propio framework advierte que puede cambiar entre versiones. La traducción ocurre en un solo sitio,
`PageCriteriaTranslator`. Los usos internos que necesitan la colección completa —los facades entre
contextos, el escenario de demostración— piden un criterio `unpaged()` que recorre el mismo camino,
de modo que no hay dos consultas que puedan discrepar en el filtro o en el orden.

La consecuencia menos obvia es la de los contadores de cabecera. «Bloqueados», «por vencer umbral» o
«sin certificación vigente» se calculaban contando la lista que el navegador ya tenía; con el
listado paginado eso describiría a la página y cambiaría al pasar a la siguiente. Ahora los mide la
base y se piden aparte, en `/equipment/summary` y `/operators/summary`. Contar en SQL es además
donde ese trabajo cuesta menos: traerse la flota entera solo para contarla anularía el motivo de
paginar.

Los índices que la paginación necesita ya estaban, porque son los mismos que sirven a los filtros:
`idx_equipment_status` y `idx_equipment_type` para la flota, `idx_shifts_status_date` para los
turnos y `idx_maintenance_records_equipment (equipment_id, performed_on DESC)` para el historial,
que cubre también su orden. El único listado que ordena sin índice propio es el de operadores, por
apellido y nombre; con una dotación de este tamaño no compensa añadirlo.

Dos detalles que se ven poco y se notan al usarlo: cambiar un filtro vuelve a la primera página (es
otro conjunto) y recargar no (es la misma vista del mismo conjunto); y si el servidor recorta el
tamaño pedido o devuelve un tramo distinto al solicitado, el paginador adopta lo que respondió en
vez de quedar señalando una página que nadie devolvió.

**Idioma.** Los identificadores están en inglés —clases, métodos, variables, nombres de tabla—
porque es la convención del ecosistema y hace que el código se lea igual que las bibliotecas con las
que convive. Los comentarios, la documentación, los mensajes de la interfaz, los textos que devuelve
la API y los registros del log están en castellano: quien opera la mina no debería tener que leer
inglés para entender por qué el sistema rechazó una asignación.

---

## 6. Qué dejé fuera, y por qué

Prioricé el núcleo evaluado sobre la superficie.

- **Recuperación de contraseña, MFA, gestión de usuarios por interfaz.** El proyecto base traía un
  módulo IAM con TOTP, SMTP y almacenamiento de archivos. Lo **reescribí** recortado a JWT + roles:
  arrastraba dominio de otro proyecto (imports a `pe.edu.upc.iaedesbackend`, entidades de personal
  sanitario) y no compilaba. Nada de eso aportaba a las reglas del reto y sí consumía tiempo y
  credenciales externas.
- **Edición y borrado de equipos y operadores.** Se crean y se cambian de estado. Borrar un equipo con
  historial de mantenimiento es una decisión de negocio que el enunciado no plantea.
- **Notificaciones fuera de la aplicación** (correo, WhatsApp) cuando un equipo se bloquea. El evento
  de dominio ya existe y tiene un suscriptor; agregar otro es directo, pero exige credenciales de un
  proveedor.
- **Reprogramación asistida.** Cuando una asignación queda en riesgo, el sistema avisa pero no sugiere
  reemplazos. Sugerir bien exige criterio (¿qué equipo?, ¿qué operador certificado y libre?) y
  merecía más tiempo del disponible.
- **Auditoría general.** Las excepciones autorizadas quedan registradas, pero no hay una bitácora
  transversal de todo cambio.

## 7. Qué haría con más tiempo

1. **Pruebas de integración con Testcontainers** para los índices únicos parciales y la carrera de dos
   supervisores. Las dependencias ya están en el `pom.xml`. Hoy la suite arranca la aplicación
   completa contra una base en memoria (`DemoScenarioIntegrationTest`), lo que valida el mapeo, las
   consultas y el escenario de negocio de punta a punta; lo que falta cubrir es específicamente el
   comportamiento concurrente contra PostgreSQL real.
2. **Reprogramación asistida** para las asignaciones en riesgo.
3. **Historial de horómetro** como serie temporal, para poder responder "¿qué leía esta máquina el
   martes?" y auditar correcciones de lectura.
4. **Métricas** (`micrometer`) sobre asignaciones rechazadas por código de regla. Saber *qué* regla se
   incumple más es información operativa: si el 70 % son certificaciones vencidas, el problema no está
   en el software.

---

## 8. Uso de IA

Usé **Claude Code (Anthropic)** como asistente durante todo el desarrollo.

**Para qué lo usé:**

- Discutir las alternativas de modelado antes de escribir código, en particular la frontera de
  agregados (asignación dentro del turno vs. agregado propio) y la política del siguiente umbral de
  mantenimiento.
- Generar el andamiaje repetitivo que la arquitectura DDD por capas impone: commands, queries,
  resources, assemblers. Es código mecánico una vez decidido el modelo.
- Escribir la primera versión de las migraciones SQL y de las pruebas unitarias, que luego revisé y
  corregí.
- Redactar este documento y el README a partir de las decisiones ya tomadas.

**Qué decidí yo:** la separación en contextos y sus fronteras; que las reglas devuelvan valores en
lugar de lanzar excepciones (que es lo que hace que la regla 11 se cumpla por construcción); la
distinción entre violaciones autorizables y no autorizables; el anclaje del ciclo de mantenimiento al
umbral y sus dos casos límite; la estrategia de tres capas para la concurrencia; y que las
asignaciones comprometidas se marquen en vez de borrarse.

**Qué revisé línea por línea:** todo lo que toca las reglas de negocio —los agregados `Equipment`,
`Shift` y `Operator`, el motor de reglas y sus siete implementaciones, el cierre de turno y la
proyección— además del esquema SQL.
