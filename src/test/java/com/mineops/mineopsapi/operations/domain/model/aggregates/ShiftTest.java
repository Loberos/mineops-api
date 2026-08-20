package com.mineops.mineopsapi.operations.domain.model.aggregates;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentStatus;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.ShiftStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.mineops.mineopsapi.operations.OperationsFixtures.HAUL_TRUCK_TYPE_ID;
import static com.mineops.mineopsapi.operations.OperationsFixtures.availableEquipment;
import static com.mineops.mineopsapi.operations.OperationsFixtures.book;
import static com.mineops.mineopsapi.operations.OperationsFixtures.certifiedOperator;
import static com.mineops.mineopsapi.operations.OperationsFixtures.dayShift;
import static com.mineops.mineopsapi.operations.OperationsFixtures.nightShift;
import static com.mineops.mineopsapi.operations.OperationsFixtures.validCertification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@DisplayName("Turno")
class ShiftTest {

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Test
    @DisplayName("deriva su inicio y su fin de la jornada y las horas planificadas")
    void derivesItsIntervalFromTheJourney() {
        var shift = dayShift(TOMORROW, 12);

        assertThat(shift.startsAt()).isEqualTo(TOMORROW.atTime(LocalTime.of(7, 0)));
        assertThat(shift.endsAt()).isEqualTo(TOMORROW.atTime(LocalTime.of(19, 0)));
        assertThat(shift.endDate()).isEqualTo(TOMORROW);
    }

    @Test
    @DisplayName("un turno de noche largo termina al día siguiente")
    void aNightShiftCrossesMidnight() {
        var shift = nightShift(TOMORROW, 12);

        assertThat(shift.startsAt()).isEqualTo(TOMORROW.atTime(LocalTime.of(19, 0)));
        assertThat(shift.endDate()).isEqualTo(TOMORROW.plusDays(1));
    }

    @Test
    @DisplayName("rechaza asignar dos veces al mismo operador")
    void refusesTheSameOperatorTwice() {
        var shift = dayShift(TOMORROW, 12);
        var operator = certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        book(shift, operator, availableEquipment(10L, "CAM-001", 100, 250));

        assertThatIllegalStateException()
                .isThrownBy(() -> book(shift, operator, availableEquipment(11L, "CAM-002", 100, 250)))
                .withMessageContaining("operador ya está asignado");
    }

    @Test
    @DisplayName("rechaza asignar dos veces la misma máquina")
    void refusesTheSameEquipmentTwice() {
        var shift = dayShift(TOMORROW, 12);
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)), equipment);

        assertThatIllegalStateException()
                .isThrownBy(() -> book(shift, certifiedOperator(2L, validCertification(HAUL_TRUCK_TYPE_ID)), equipment))
                .withMessageContaining("equipo ya está asignado");
    }

    @Test
    @DisplayName("marca las asignaciones de una máquina que ya no puede trabajar, en vez de borrarlas")
    void flagsTheAssignmentsOfABlockedMachine() {
        var shift = dayShift(TOMORROW, 12);
        var equipment = availableEquipment(10L, "CAM-001", 240, 250);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)), equipment);

        var flagged = shift.flagAssignmentsForEquipment(equipment.id(), "Bloqueado con 254 horas");

        assertThat(flagged).isEqualTo(1);
        assertThat(shift.getAssignments()).hasSize(1);
        var assignment = shift.getAssignments().getFirst();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.AT_RISK);
        assertThat(assignment.getRiskReason()).contains("254");
    }

    @Test
    @DisplayName("levanta la marca cuando la máquina vuelve a servicio")
    void liftsTheFlagWhenTheMachineReturns() {
        var shift = dayShift(TOMORROW, 12);
        var equipment = availableEquipment(10L, "CAM-001", 240, 250);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)), equipment);
        shift.flagAssignmentsForEquipment(equipment.id(), "Bloqueado");

        shift.clearRiskForEquipment(equipment.id());

        var assignment = shift.getAssignments().getFirst();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.SCHEDULED);
        assertThat(assignment.getRiskReason()).isNull();
    }

    @Test
    @DisplayName("una máquina en riesgo sigue ocupando su lugar en el turno")
    void anAssignmentAtRiskStillOccupiesItsSlot() {
        var shift = dayShift(TOMORROW, 12);
        var equipment = availableEquipment(10L, "CAM-001", 240, 250);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)), equipment);
        shift.flagAssignmentsForEquipment(equipment.id(), "Bloqueado");

        assertThat(shift.hasEquipmentAssigned(equipment.id())).isTrue();
    }

    @Test
    @DisplayName("rechaza liquidarse mientras una asignación siga abierta")
    void refusesToCloseWithOpenAssignments() {
        var shift = dayShift(TOMORROW, 12);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)),
                availableEquipment(10L, "CAM-001", 100, 250));

        assertThatIllegalStateException()
                .isThrownBy(shift::close)
                .withMessageContaining("Toda asignación debe liquidarse");
    }

    @Test
    @DisplayName("se liquida una vez que todas sus asignaciones lo están")
    void closesWhenEveryAssignmentIsSettled() {
        var shift = dayShift(TOMORROW, 12);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)),
                availableEquipment(10L, "CAM-001", 100, 250));
        shift.getAssignments().getFirst().complete(BigDecimal.valueOf(11), "Parada por lluvia");

        shift.close();

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.CLOSED);
        assertThat(shift.getClosedAt()).isNotNull();
        assertThat(shift.isOpen()).isFalse();
    }

    @Test
    @DisplayName("informa cuánto se apartó del plan una liquidación")
    void reportsTheDepartureFromThePlan() {
        var shift = dayShift(TOMORROW, 12);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)),
                availableEquipment(10L, "CAM-001", 100, 250));
        var assignment = shift.getAssignments().getFirst();

        assignment.complete(BigDecimal.valueOf(14), "Sobretiempo autorizado");

        assertThat(assignment.hoursVariance()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @Test
    @DisplayName("libera todas sus asignaciones cuando se suspende el turno completo")
    void releasesEverythingWhenCancelled() {
        var shift = dayShift(TOMORROW, 12);
        book(shift, certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)),
                availableEquipment(10L, "CAM-001", 100, 250));

        shift.cancel("Paro de operaciones");

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.CANCELLED);
        assertThat(shift.getAssignments()).allMatch(a -> a.getStatus() == AssignmentStatus.CANCELLED);
        assertThat(shift.activeAssignments()).isEmpty();
    }

    @Test
    @DisplayName("ya no acepta asignaciones una vez liquidado")
    void refusesAssignmentsOnceSettled() {
        var shift = dayShift(TOMORROW, 12);
        shift.close();

        assertThatIllegalStateException().isThrownBy(() -> book(
                shift,
                certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID)),
                availableEquipment(10L, "CAM-001", 100, 250)));
    }
}
