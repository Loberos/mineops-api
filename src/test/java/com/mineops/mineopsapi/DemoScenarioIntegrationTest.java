package com.mineops.mineopsapi;

import com.mineops.mineopsapi.assets.domain.model.queries.GetAllEquipmentQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetEquipmentByCodeQuery;
import com.mineops.mineopsapi.assets.domain.model.queries.GetMaintenanceHistoryQuery;
import com.mineops.mineopsapi.assets.domain.model.valueobjects.EquipmentStatus;
import com.mineops.mineopsapi.assets.domain.services.EquipmentQueryService;
import com.mineops.mineopsapi.assets.domain.services.MaintenanceQueryService;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAllShiftsQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.GetAssignmentsAtRiskQuery;
import com.mineops.mineopsapi.operations.domain.model.queries.GetMaintenanceProjectionQuery;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.domain.services.AssignmentQueryService;
import com.mineops.mineopsapi.operations.domain.services.MaintenanceProjectionQueryService;
import com.mineops.mineopsapi.operations.domain.services.ShiftQueryService;
import com.mineops.mineopsapi.workforce.domain.model.queries.GetOperatorByDocumentNumberQuery;
import com.mineops.mineopsapi.workforce.domain.services.OperatorQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta la aplicación completa y verifica la operación de demostración que carga.
 * <p>
 * Se comprueban dos cosas a la vez. Primero, que todo se cablea y se mapea: un mapeo JPA roto, una
 * consulta que no compila o un bean faltante fallan aquí y no delante de quien evalúa. Segundo, y más
 * interesante, que los casos incómodos que pide el reto son genuinamente alcanzables a través de los
 * comandos reales: el cargador emite los mismos comandos que emitiría un usuario, así que si las
 * reglas no se comportaran como se pretende, estos datos no habrían podido construirse siquiera.
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Operación de demostración")
class DemoScenarioIntegrationTest {

    @Autowired
    private EquipmentQueryService equipmentQueryService;

    @Autowired
    private MaintenanceQueryService maintenanceQueryService;

    @Autowired
    private OperatorQueryService operatorQueryService;

    @Autowired
    private ShiftQueryService shiftQueryService;

    @Autowired
    private AssignmentQueryService assignmentQueryService;

    @Autowired
    private MaintenanceProjectionQueryService maintenanceProjectionQueryService;

    @Test
    @DisplayName("carga una flota con todas las familias de máquinas representadas")
    void loadsTheFleet() {
        var fleet = equipmentQueryService.handle(GetAllEquipmentQuery.unfiltered());

        assertThat(fleet).hasSize(11);
        assertThat(fleet).extracting(equipment -> equipment.getEquipmentType().getCode())
                .contains("HAUL_TRUCK", "EXCAVATOR", "DRILL");
    }

    @Test
    @DisplayName("tiene una máquina a pocas horas de su umbral de mantenimiento")
    void hasAMachineAboutToReachMaintenance() {
        var truck = equipmentQueryService.handle(new GetEquipmentByCodeQuery("CAM-001")).orElseThrow();

        assertThat(truck.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
        assertThat(truck.hoursUntilMaintenance())
                .isPositive()
                .isLessThanOrEqualTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("tiene un operador cuya certificación ya venció")
    void hasAnOperatorWithAnExpiredCertification() {
        var operator = operatorQueryService
                .handle(new GetOperatorByDocumentNumberQuery("45678902"))
                .orElseThrow();

        assertThat(operator.getCertifications()).isNotEmpty();
        assertThat(operator.getCertifications())
                .noneMatch(certification -> certification.isValidOn(LocalDate.now()));
    }

    @Test
    @DisplayName("liquidar el turno de ayer bloqueó la perforadora que cruzó su umbral")
    void settlingAShiftBlockedTheMachineThatCrossedItsThreshold() {
        var drill = equipmentQueryService.handle(new GetEquipmentByCodeQuery("PER-001")).orElseThrow();

        assertThat(drill.getStatus()).isEqualTo(EquipmentStatus.BLOCKED);
        assertThat(drill.isAvailableForAssignment()).isFalse();
        assertThat(drill.hoursUntilMaintenance()).isLessThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("los turnos ya programados con esa perforadora quedaron marcados, no borrados")
    void theShiftsBookedForABlockedMachineWereFlagged() {
        var atRisk = assignmentQueryService.handle(new GetAssignmentsAtRiskQuery());

        assertThat(atRisk).isNotEmpty();
        assertThat(atRisk).allMatch(assignment -> assignment.getStatus() == AssignmentStatus.AT_RISK);
        assertThat(atRisk).anyMatch(assignment -> assignment.getEquipmentCode().equals("PER-001"));
        assertThat(atRisk).allSatisfy(assignment ->
                assertThat(assignment.getRiskReason()).isNotBlank());
    }

    @Test
    @DisplayName("conserva la traza de auditoría de las asignaciones que autorizó un supervisor")
    void keepsTheAuditTrailOfForcedAssignments() {
        var forced = shiftQueryService.handle(GetAllShiftsQuery.unfiltered()).stream()
                .flatMap(shift -> shift.getAssignments().stream())
                .filter(assignment -> assignment.getAuthorization() != null)
                .toList();

        assertThat(forced).isNotEmpty();
        assertThat(forced).allSatisfy(assignment -> {
            var authorization = assignment.getAuthorization();
            assertThat(authorization.getAuthorizedByUserId()).isNotNull();
            assertThat(authorization.getReason()).isNotBlank();
            assertThat(authorization.getAuthorizedAt()).isNotNull();
            // Las reglas que se omitieron quedan congeladas en el momento de la decisión.
            assertThat(authorization.getOverriddenRuleCodeList()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("registra un mantenimiento tardío con su desfase, absorbiendo el atraso en el siguiente ciclo")
    void recordsALateMaintenanceWithoutCarryingTheDelayForward() {
        var history = maintenanceQueryService.handle(GetMaintenanceHistoryQuery.forFleet());

        assertThat(history).isNotEmpty();
        var lateMaintenance = history.stream()
                .filter(record -> record.getEquipmentCode().equals("CAM-002"))
                .findFirst()
                .orElseThrow();

        assertThat(lateMaintenance.wasOverdue()).isTrue();
        assertThat(lateMaintenance.getOverrunHours()).isEqualByComparingTo(BigDecimal.valueOf(10));
        // Atendida con 260 frente a un umbral de 250: la siguiente parada es 500, no 510.
        assertThat(lateMaintenance.getNextThresholdHours()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("proyecta qué máquinas llevará la programación más allá de su umbral")
    void projectsTheMachinesThatWillReachMaintenance() {
        var projection = maintenanceProjectionQueryService.handle(new GetMaintenanceProjectionQuery(7));

        assertThat(projection).isNotEmpty();
        // La perforadora ya está detenida, así que encabeza la lista sin importar lo que tenga programado.
        assertThat(projection.getFirst().alreadyBlocked()).isTrue();
        // Y al menos una máquina en marcha es llevada más allá de su umbral por los turnos que vienen.
        assertThat(projection)
                .anyMatch(row -> !row.alreadyBlocked() && row.willReachThreshold() && row.crossingDate() != null);
    }
}
