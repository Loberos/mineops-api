package com.mineops.mineopsapi.shared.application.internal.seeding;

import com.mineops.mineopsapi.assets.domain.model.aggregates.Equipment;
import com.mineops.mineopsapi.assets.domain.model.aggregates.EquipmentType;
import com.mineops.mineopsapi.assets.domain.model.commands.ChangeEquipmentStatusCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.CreateEquipmentTypeCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.RegisterEquipmentUsageCommand;
import com.mineops.mineopsapi.assets.domain.model.commands.RegisterMaintenanceCommand;
import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentTypesQuery;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.domain.services.EquipmentCommandService;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeCommandService;
import com.mineops.mineopsapi.assets.domain.services.EquipmentTypeQueryService;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceCommandService;
import com.mineops.mineopsapi.iam.domain.model.commands.SignUpCommand;
import com.mineops.mineopsapi.iam.domain.model.entities.Role;
import com.mineops.mineopsapi.iam.domain.services.UserCommandService;
import com.mineops.mineopsapi.operations.domain.model.commands.AssignOperatorToShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CancelAssignmentCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CloseShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.commands.CreateShiftCommand;
import com.mineops.mineopsapi.operations.domain.model.queries.GetShiftByIdQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.Journey;
import com.mineops.mineopsapi.operations.domain.services.AssignmentCommandService;
import com.mineops.mineopsapi.operations.domain.services.ShiftCommandService;
import com.mineops.mineopsapi.operations.domain.services.ShiftQueryService;
import com.mineops.mineopsapi.workforce.domain.model.aggregates.Operator;
import com.mineops.mineopsapi.workforce.domain.model.commands.ChangeOperatorStatusCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.CreateOperatorCommand;
import com.mineops.mineopsapi.workforce.domain.model.commands.GrantCertificationCommand;
import com.mineops.mineopsapi.workforce.domain.model.valueobjects.OperatorStatus;
import com.mineops.mineopsapi.workforce.domain.services.OperatorCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga una operación de demostración para poder ejercitar el sistema sin escribir nada a mano.
 * <p>
 * El conjunto de datos no es decorativo: está construido para contener los casos incómodos a
 * propósito, y los construye emitiendo los mismos comandos que emitiría un usuario, nunca escribiendo
 * filas directamente. Así se garantiza que los datos sean alcanzables a través de las reglas reales, y
 * la carga misma funciona como prueba de humo de ellas.
 * </p>
 * <p>
 * Lo que prepara deliberadamente:
 * </p>
 * <ul>
 *   <li><strong>Una máquina a punto de alcanzar su mantenimiento</strong> — CAM-001, a ocho horas de
 *       su umbral.</li>
 *   <li><strong>Un operador con certificación vencida</strong> — María Huamán, cuya licencia de camión
 *       venció hace diez días.</li>
 *   <li><strong>Un turno que al cerrarse bloquea una máquina</strong> — el turno de día de hoy, dejado
 *       abierto a propósito para que quien evalúe lo cierre y vea detenerse a CAM-001.</li>
 *   <li><strong>Una certificación que vence a mitad de un turno futuro</strong> — Carlos Mamani en el
 *       turno de noche del día en que vence su licencia.</li>
 *   <li><strong>Una máquina bloqueada a mitad de semana con turnos ya programados</strong> — PER-001
 *       cruza su umbral al liquidarse el turno de ayer, y las asignaciones que la esperaban más
 *       adelante en la semana quedan marcadas en vez de borradas.</li>
 *   <li><strong>Una asignación forzada</strong> — firmada por el supervisor, con las reglas omitidas
 *       registradas.</li>
 *   <li><strong>Un mantenimiento hecho tarde</strong> — CAM-002, atendido diez horas después de su
 *       umbral, de modo que el siguiente ciclo muestra cómo se absorbe el atraso en lugar de
 *       arrastrarlo.</li>
 *   <li><strong>Un mantenimiento hecho antes de tiempo</strong> — CAM-004, atendido a las 120 h de un
 *       umbral de 250. El caso contrario al anterior: aquí el ciclo sí se cuenta desde el horómetro
 *       real, porque anclarlo al umbral castigaría a quien atiende la máquina a tiempo.</li>
 *   <li><strong>Un turno cerrado con más horas de las planificadas</strong> — el de anteayer, ocho
 *       horas programadas y diez trabajadas, aceptado solo porque la desviación viene justificada.</li>
 *   <li><strong>Una certificación que vence hoy mismo</strong> — Pedro Condori en perforadora. El
 *       borde exacto de un rango cerrado en ambos extremos: hoy todavía cubre.</li>
 *   <li><strong>Máquinas detenidas por decisión de una persona</strong> — EXC-003 en el taller y
 *       PER-003 dada de baja. Ninguna puede asignarse, y por un motivo distinto al bloqueo por
 *       umbral.</li>
 *   <li><strong>Un operador fuera de la plantilla</strong> — Luis Apaza, inactivo: no puede
 *       programarse ni con autorización de un supervisor.</li>
 *   <li><strong>Una asignación cancelada</strong> — sobrevive como historia y libera al operador y a
 *       la máquina, que es la razón por la que los índices únicos son parciales.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "mineops.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String DEMO_PASSWORD = "MineOps2026!";
    private static final String SUPERVISOR_EMAIL = "supervisor@mineops.pe";

    private static final String HAUL_TRUCK = "HAUL_TRUCK";
    private static final String EXCAVATOR = "EXCAVATOR";
    private static final String DRILL = "DRILL";

    private final UserCommandService userCommandService;
    private final EquipmentTypeCommandService equipmentTypeCommandService;
    private final EquipmentTypeQueryService equipmentTypeQueryService;
    private final EquipmentCommandService equipmentCommandService;
    private final MaintenanceCommandService maintenanceCommandService;
    private final OperatorCommandService operatorCommandService;
    private final ShiftCommandService shiftCommandService;
    private final ShiftQueryService shiftQueryService;
    private final AssignmentCommandService assignmentCommandService;

    public DemoDataSeeder(
            UserCommandService userCommandService,
            EquipmentTypeCommandService equipmentTypeCommandService,
            EquipmentTypeQueryService equipmentTypeQueryService,
            EquipmentCommandService equipmentCommandService,
            MaintenanceCommandService maintenanceCommandService,
            OperatorCommandService operatorCommandService,
            ShiftCommandService shiftCommandService,
            ShiftQueryService shiftQueryService,
            AssignmentCommandService assignmentCommandService) {
        this.userCommandService = userCommandService;
        this.equipmentTypeCommandService = equipmentTypeCommandService;
        this.equipmentTypeQueryService = equipmentTypeQueryService;
        this.equipmentCommandService = equipmentCommandService;
        this.maintenanceCommandService = maintenanceCommandService;
        this.operatorCommandService = operatorCommandService;
        this.shiftCommandService = shiftCommandService;
        this.shiftQueryService = shiftQueryService;
        this.assignmentCommandService = assignmentCommandService;
    }

    /**
     * Corre después de haberse verificado el catálogo de roles, y solo contra una base vacía, de modo
     * que un reinicio nunca duplica la demostración ni pisa datos reales.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    @Transactional
    public void seed(ApplicationReadyEvent event) {
        if (!equipmentTypeQueryService.handle(new GetAllEquipmentTypesQuery()).isEmpty()) {
            LOGGER.info("La base ya tiene datos; no se cargó el conjunto de demostración");
            return;
        }

        LOGGER.info("Cargando el conjunto de datos de demostración");
        seedUsers();

        var haulTruck = createEquipmentType(HAUL_TRUCK, "Camión de acarreo", 250, "Acarreo de mineral y desmonte");
        var excavator = createEquipmentType(EXCAVATOR, "Excavadora", 500, "Carguío en frente de mina");
        var drill = createEquipmentType(DRILL, "Perforadora", 300, "Perforación para voladura");

        // Las máquinas entran a la flota con el horómetro en cero y luego se envejecen con el mismo
        // comando que emite un turno cerrado, para que sus umbrales sean los que realmente habrían
        // producido las reglas.
        var truck1 = createEquipment("CAM-001", haulTruck, 242);   // a ocho horas de detenerse
        var truck2 = createEquipment("CAM-002", haulTruck, 260);   // ya pasada de su umbral: se atiende abajo
        var truck3 = createEquipment("CAM-003", haulTruck, 60);
        var excavator1 = createEquipment("EXC-001", excavator, 180);
        var excavator2 = createEquipment("EXC-002", excavator, 495); // a cinco horas de detenerse
        var drill1 = createEquipment("PER-001", drill, 290);        // cruza su umbral al liquidarse ayer
        createEquipment("PER-002", drill, 90);

        // Flota adicional. No es relleno: da volumen suficiente para que la proyección muestre varias
        // máquinas cruzando su umbral en momentos distintos, que es donde se ve que recorre el
        // calendario en orden y no se limita a sumar las horas de la semana.
        var truck4 = createEquipment("CAM-004", haulTruck, 120);
        var truck5 = createEquipment("CAM-005", haulTruck, 230);   // cruza su umbral pasado mañana
        var excavator3 = createEquipment("EXC-003", excavator, 320);
        var drill3 = createEquipment("PER-003", drill, 150);

        // A CAM-002 se le hizo mantenimiento diez horas después de lo que correspondía. Registrar la
        // lectura real es lo que permite que el siguiente umbral absorba el atraso en vez de empujarlo
        // hacia adelante para siempre.
        maintenanceCommandService.handle(new RegisterMaintenanceCommand(
                truck2.getId(),
                LocalDate.now().minusDays(3),
                BigDecimal.valueOf(260),
                "Taller Mecánico Central",
                "Mantenimiento preventivo de 250 h realizado con 10 h de atraso. "
                        + "Cambio de aceite, filtros y revisión de frenos."));

        // El caso opuesto, que la regla ingenua también rompe: CAM-004 entró al taller **antes** de su
        // umbral. Aquí el siguiente ciclo sí se cuenta desde el horómetro real (120 + 250 = 370),
        // porque anclarlo al umbral castigaría con un ciclo más corto a quien atiende a tiempo.
        maintenanceCommandService.handle(new RegisterMaintenanceCommand(
                truck4.getId(),
                LocalDate.now().minusDays(6),
                BigDecimal.valueOf(120),
                "Taller Mecánico Central",
                "Mantenimiento adelantado, aprovechando una parada de planta."));

        // Dos estados que no decide el horómetro sino una persona: una máquina en el taller y otra
        // dada de baja. Ninguna puede asignarse, y por motivos distintos al bloqueo por umbral.
        equipmentCommandService.handle(
                new ChangeEquipmentStatusCommand(excavator3.getId(), EquipmentStatus.IN_MAINTENANCE));
        equipmentCommandService.handle(
                new ChangeEquipmentStatusCommand(drill3.getId(), EquipmentStatus.OUT_OF_SERVICE));

        var juan = createOperator("45678901", "Juan", "Quispe");
        var maria = createOperator("45678902", "María", "Huamán");
        var carlos = createOperator("45678903", "Carlos", "Mamani");
        var rosa = createOperator("45678904", "Rosa", "Ccama");
        var luis = createOperator("45678905", "Luis", "Apaza");
        var pedro = createOperator("45678906", "Pedro", "Condori");
        var ana = createOperator("45678907", "Ana", "Ticona");

        var today = LocalDate.now();

        certify(juan, haulTruck, today.minusYears(1), today.plusMonths(10));
        certify(juan, excavator, today.minusYears(1), today.plusMonths(8));
        // Vencida hace diez días: asignarla a un camión se rechaza, y es el caso que un supervisor
        // puede optar por autorizar.
        certify(maria, haulTruck, today.minusYears(2), today.minusDays(10));
        certify(carlos, drill, today.minusYears(1), today.plusMonths(6));
        certify(carlos, excavator, today.minusYears(1), today.plusMonths(4));
        // Vence en tres días. El turno de noche de ese día termina a la mañana siguiente, así que la
        // certificación cubre su inicio y no su final.
        certify(carlos, haulTruck, today.minusYears(1), today.plusDays(3));
        certify(rosa, excavator, today.minusYears(1), today.plusMonths(11));
        certify(luis, haulTruck, today.minusYears(1), today.plusMonths(9));
        certify(pedro, haulTruck, today.minusYears(1), today.plusMonths(7));
        // Vence hoy mismo. Los rangos son cerrados en ambos extremos, así que hoy **todavía** cubre:
        // es el borde exacto en el que una comparación mal escrita cambiaría la respuesta.
        certify(pedro, drill, today.minusYears(1), today);
        certify(ana, excavator, today.minusYears(1), today.plusMonths(5));
        certify(ana, haulTruck, today.minusYears(1), today.plusMonths(3));
        // Luis está fuera de la plantilla, así que no puede programarse en absoluto, por nadie.
        operatorCommandService.handle(new ChangeOperatorStatusCommand(luis.getId(), OperatorStatus.INACTIVE));

        var twoDaysAgoDay = createShift(
                today.minusDays(2), Journey.DAY, 8, "Planificado a 8 h, se trabajaron 10");
        var yesterdayDay = createShift(today.minusDays(1), Journey.DAY, 12, "Turno trabajado, pendiente de cierre");
        var todayDay = createShift(today, Journey.DAY, 12, "Al cerrarlo, CAM-001 y EXC-002 alcanzan su umbral");
        var todayNight = createShift(today, Journey.NIGHT, 12, null);
        var tomorrowDay = createShift(today.plusDays(1), Journey.DAY, 12, null);
        var tomorrowNight = createShift(today.plusDays(1), Journey.NIGHT, 12, null);
        var inThreeDaysNight = createShift(
                today.plusDays(3), Journey.NIGHT, 12, "La certificación de Carlos vence en pleno turno");
        var inTwoDaysDay = createShift(today.plusDays(2), Journey.DAY, 12, null);
        var inTwoDaysNight = createShift(today.plusDays(2), Journey.NIGHT, 12, null);
        var inFourDaysDay = createShift(today.plusDays(4), Journey.DAY, 12, null);
        var inFiveDaysDay = createShift(today.plusDays(5), Journey.DAY, 12, null);

        // Anteayer: un turno corto que terminó alargándose. Se liquida más abajo con las horas reales.
        assign(twoDaysAgoDay, pedro, truck4);
        assign(twoDaysAgoDay, ana, truck5);

        // Ayer: la perforadora trabaja sus últimas horas antes de cruzar su umbral.
        assign(yesterdayDay, carlos, drill1);
        assign(yesterdayDay, juan, truck3);

        // Hoy: dos máquinas alcanzarán su umbral cuando se liquide este turno.
        assign(todayDay, juan, truck1);
        assign(todayDay, rosa, excavator2);

        assign(todayNight, carlos, excavator1);

        // Más adelante en la semana la perforadora vuelve a estar programada. Cuando se liquide el
        // turno de ayer la perforadora se bloquea, y estas asignaciones quedan marcadas en vez de
        // desaparecer.
        assign(tomorrowDay, carlos, drill1);
        assign(tomorrowDay, juan, truck2);
        assign(inFourDaysDay, carlos, drill1);

        // La semana programada con la flota ampliada. CAM-005 cruza su umbral en el turno de pasado
        // mañana, y CAM-001 está comprometido el día cinco: cuando quien evalúe cierre el turno de hoy
        // y lo bloquee, verá esa asignación pasar a estar en riesgo sin haber tocado nada más.
        assign(inTwoDaysDay, pedro, truck5);
        assign(inTwoDaysDay, ana, excavator1);
        assign(inTwoDaysNight, juan, truck4);
        assign(inFiveDaysDay, pedro, truck1);

        // Una asignación que alguien canceló. La fila sobrevive como historia y libera al operador y a
        // la máquina para un reemplazo: por eso los índices únicos de `assignments` son parciales y no
        // totales, y por eso reprogramar no obliga a borrar nada.
        assign(inFiveDaysDay, ana, truck3);
        assignmentCommandService.handle(new CancelAssignmentCommand(
                inFiveDaysDay,
                assignmentIdOf(inFiveDaysDay, truck3.getId()),
                "Reprogramada a pedido del jefe de guardia"));

        // Un supervisor programa a María en un camión sabiendo que su certificación venció, y lo firma.
        forceAssign(
                tomorrowNight,
                maria,
                truck3,
                "Ausencia imprevista del titular. Se autoriza por única vez; la recertificación ya está "
                        + "solicitada al área de seguridad.");

        // Carlos en la noche en que vence su certificación de camión: vigente al empezar el turno,
        // caducada para cuando termina.
        forceAssign(
                inThreeDaysNight,
                carlos,
                truck1,
                "Se programa a pesar de que la certificación vence durante el turno; "
                        + "relevo confirmado a las 00:00.");

        // El turno de anteayer se planificó a 8 h y se trabajaron 10. Manda lo trabajado —esas son las
        // horas que la máquina realmente acumuló—, y como la desviación supera la tolerancia, el
        // cierre solo se acepta porque viene justificado por escrito.
        shiftCommandService.handle(new CloseShiftCommand(twoDaysAgoDay, List.of(
                new CloseShiftCommand.AssignmentClosure(
                        assignmentIdOf(twoDaysAgoDay, truck4.getId()), BigDecimal.valueOf(10),
                        "Se extendió el turno para terminar el carguío del frente norte"),
                new CloseShiftCommand.AssignmentClosure(
                        assignmentIdOf(twoDaysAgoDay, truck5.getId()), BigDecimal.valueOf(10),
                        "Se extendió el turno para terminar el carguío del frente norte"))));

        // Liquidar el turno de ayer es lo que lleva a PER-001 más allá de su umbral y deja en riesgo el
        // resto de su semana. Se hace al final para que el efecto sea visible en los datos que abre
        // quien evalúe.
        shiftCommandService.handle(new CloseShiftCommand(yesterdayDay, List.of(
                new CloseShiftCommand.AssignmentClosure(
                        assignmentIdOf(yesterdayDay, drill1.getId()), BigDecimal.valueOf(12), null),
                new CloseShiftCommand.AssignmentClosure(
                        assignmentIdOf(yesterdayDay, truck3.getId()), BigDecimal.valueOf(10),
                        "Parada por lluvia intensa en el frente de trabajo"))));

        LOGGER.info("Conjunto de demostración cargado. Ingresa con {} y la contraseña documentada en el README",
                SUPERVISOR_EMAIL);
    }

    private void seedUsers() {
        signUp("admin@mineops.pe", "Ana Delgado", "ADMIN");
        signUp(SUPERVISOR_EMAIL, "Roberto Salas", "SUPERVISOR");
        signUp("planner@mineops.pe", "Elena Ríos", "PLANNER");
        signUp("viewer@mineops.pe", "Marco Tapia", "VIEWER");
    }

    private void signUp(String email, String fullName, String role) {
        userCommandService.handle(
                new SignUpCommand(email, DEMO_PASSWORD, fullName, List.of(Role.toRoleFromName(role))));
    }

    private EquipmentType createEquipmentType(String code, String name, int intervalHours, String description) {
        return equipmentTypeCommandService
                .handle(new CreateEquipmentTypeCommand(
                        code, name, BigDecimal.valueOf(intervalHours), description))
                .orElseThrow();
    }

    private Equipment createEquipment(String code, EquipmentType type, int hoursOfUse) {
        var equipment = equipmentCommandService
                .handle(new CreateEquipmentCommand(code, type.getId(), BigDecimal.ZERO))
                .orElseThrow();
        if (hoursOfUse <= 0) {
            return equipment;
        }
        return equipmentCommandService
                .handle(new RegisterEquipmentUsageCommand(equipment.getId(), BigDecimal.valueOf(hoursOfUse)))
                .orElseThrow();
    }

    private Operator createOperator(String documentNumber, String firstName, String lastName) {
        return operatorCommandService
                .handle(new CreateOperatorCommand(documentNumber, firstName, lastName))
                .orElseThrow();
    }

    private void certify(Operator operator, EquipmentType type, LocalDate issuedOn, LocalDate expiresOn) {
        operatorCommandService.handle(
                new GrantCertificationCommand(operator.getId(), type.getId(), issuedOn, expiresOn));
    }

    private Long createShift(LocalDate date, Journey journey, int plannedHours, String notes) {
        return shiftCommandService
                .handle(new CreateShiftCommand(date, journey, BigDecimal.valueOf(plannedHours), notes))
                .orElseThrow()
                .getId();
    }

    private void assign(Long shiftId, Operator operator, Equipment equipment) {
        assignmentCommandService.handle(AssignOperatorToShiftCommand.plain(
                shiftId, operator.getId(), equipment.getId(), SUPERVISOR_EMAIL));
    }

    private void forceAssign(Long shiftId, Operator operator, Equipment equipment, String reason) {
        assignmentCommandService.handle(new AssignOperatorToShiftCommand(
                shiftId, operator.getId(), equipment.getId(), true, reason, SUPERVISOR_EMAIL));
    }

    /**
     * Busca la asignación de una máquina en un turno, para que el cierre pueda nombrarla.
     */
    private Long assignmentIdOf(Long shiftId, Long equipmentId) {
        return shiftQueryService.handle(new GetShiftByIdQuery(shiftId))
                .orElseThrow()
                .getAssignments().stream()
                .filter(assignment -> assignment.isForEquipment(equipmentId))
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
