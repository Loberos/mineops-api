package com.mineops.mineopsapi.operations.domain.services;

import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentContext;
import com.mineops.mineopsapi.operations.domain.model.valueobjects.AssignmentRuleCode;
import com.mineops.mineopsapi.operations.domain.services.rules.AssignmentRule;
import com.mineops.mineopsapi.operations.domain.services.rules.EquipmentMustBeAvailableRule;
import com.mineops.mineopsapi.operations.domain.services.rules.EquipmentNotAlreadyAssignedRule;
import com.mineops.mineopsapi.operations.domain.services.rules.EquipmentThresholdWarningRule;
import com.mineops.mineopsapi.operations.domain.services.rules.OperatorMustBeActiveRule;
import com.mineops.mineopsapi.operations.domain.services.rules.OperatorMustBeCertifiedRule;
import com.mineops.mineopsapi.operations.domain.services.rules.OperatorNotAlreadyAssignedRule;
import com.mineops.mineopsapi.operations.domain.services.rules.ShiftMustBeOpenRule;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.BusinessRuleViolation;
import com.mineops.mineopsapi.shared.domain.model.valueobjects.RuleSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.mineops.mineopsapi.operations.OperationsFixtures.EXCAVATOR_TYPE_ID;
import static com.mineops.mineopsapi.operations.OperationsFixtures.HAUL_TRUCK_TYPE_ID;
import static com.mineops.mineopsapi.operations.OperationsFixtures.availableEquipment;
import static com.mineops.mineopsapi.operations.OperationsFixtures.blockedEquipment;
import static com.mineops.mineopsapi.operations.OperationsFixtures.book;
import static com.mineops.mineopsapi.operations.OperationsFixtures.certification;
import static com.mineops.mineopsapi.operations.OperationsFixtures.certifiedOperator;
import static com.mineops.mineopsapi.operations.OperationsFixtures.dayShift;
import static com.mineops.mineopsapi.operations.OperationsFixtures.expiredCertification;
import static com.mineops.mineopsapi.operations.OperationsFixtures.inactiveOperator;
import static com.mineops.mineopsapi.operations.OperationsFixtures.nightShift;
import static com.mineops.mineopsapi.operations.OperationsFixtures.validCertification;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * El motor de reglas es la pieza sobre la que gira todo el reto, así que se prueba contra las
 * implementaciones reales de las reglas y no contra sustitutos.
 */
@DisplayName("Motor de reglas de asignación")
class AssignmentRuleEvaluatorTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final AssignmentRuleEvaluator evaluator = new AssignmentRuleEvaluator(allRules());

    private static List<AssignmentRule> allRules() {
        return List.of(
                new ShiftMustBeOpenRule(),
                new OperatorMustBeActiveRule(),
                new OperatorNotAlreadyAssignedRule(),
                new EquipmentNotAlreadyAssignedRule(),
                new EquipmentMustBeAvailableRule(),
                new OperatorMustBeCertifiedRule(),
                new EquipmentThresholdWarningRule());
    }

    private static List<String> codesOf(List<BusinessRuleViolation> violations) {
        return violations.stream().map(BusinessRuleViolation::code).toList();
    }

    @Test
    @DisplayName("acepta una asignación que cumple todas las reglas")
    void acceptsAValidAssignment() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(evaluation.isAccepted()).isTrue();
        assertThat(evaluation.violations()).isEmpty();
    }

    @Test
    @DisplayName("informa todas las reglas incumplidas, no solo la primera")
    void reportsEveryBrokenRule() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, expiredCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = blockedEquipment(10L, "CAM-001");

        // El operador y la máquina ya están asignados en este turno, y encima la máquina está
        // bloqueada y la certificación vencida. Cuatro reglas, una sola petición.
        book(shift, operator, equipment);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(codesOf(evaluation.violations())).containsExactlyInAnyOrder(
                AssignmentRuleCode.OPERATOR_ALREADY_ASSIGNED.name(),
                AssignmentRuleCode.EQUIPMENT_ALREADY_ASSIGNED.name(),
                AssignmentRuleCode.EQUIPMENT_NOT_AVAILABLE.name(),
                AssignmentRuleCode.OPERATOR_CERTIFICATION_EXPIRED.name());
        assertThat(evaluation.isAccepted()).isFalse();
    }

    @Test
    @DisplayName("no permite que nadie autorice una máquina que ya conduce otra persona")
    void refusesToAuthoriseAPhysicallyImpossibleRoster() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);
        var alreadyBooked = certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        var newcomer = certifiedOperator(2L, validCertification(HAUL_TRUCK_TYPE_ID));
        book(shift, alreadyBooked, equipment);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, newcomer, equipment));

        assertThat(codesOf(evaluation.violations()))
                .containsExactly(AssignmentRuleCode.EQUIPMENT_ALREADY_ASSIGNED.name());
        assertThat(evaluation.canBeOverridden()).isFalse();
        assertThat(evaluation.nonOverridableViolations()).hasSize(1);
    }

    @Test
    @DisplayName("permite a un supervisor autorizar una máquina bloqueada y una certificación vencida")
    void allowsASupervisorToAuthoriseTheWaivableCases() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, expiredCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = blockedEquipment(10L, "CAM-001");

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(evaluation.isAccepted()).isFalse();
        assertThat(evaluation.canBeOverridden()).isTrue();
        assertThat(evaluation.blockingViolations()).allMatch(BusinessRuleViolation::overridable);
    }

    @Test
    @DisplayName("rechaza a un operador sin certificación para esa familia de máquinas")
    void rejectsAnOperatorCertifiedForSomethingElse() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, validCertification(EXCAVATOR_TYPE_ID));
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(codesOf(evaluation.violations()))
                .contains(AssignmentRuleCode.OPERATOR_NOT_CERTIFIED.name());
    }

    @Test
    @DisplayName("rechaza a un operador que dejó la plantilla, y no deja que nadie lo autorice")
    void rejectsAnInactiveOperator() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = inactiveOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(codesOf(evaluation.violations()))
                .containsExactly(AssignmentRuleCode.OPERATOR_INACTIVE.name());
        assertThat(evaluation.canBeOverridden()).isFalse();
    }

    @Test
    @DisplayName("rechaza una certificación que caduca a mitad de un turno de noche")
    void rejectsACertificationThatLapsesDuringTheShift() {
        // El turno empieza a las 19:00 del día en que vence la certificación y termina a la mañana siguiente.
        var expiryDay = TODAY.plusDays(3);
        var shift = nightShift(expiryDay, 12);
        var operator = certifiedOperator(
                1L, certification(HAUL_TRUCK_TYPE_ID, TODAY.minusYears(1), expiryDay));
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(codesOf(evaluation.violations()))
                .contains(AssignmentRuleCode.CERTIFICATION_EXPIRES_DURING_SHIFT.name());
    }

    @Test
    @DisplayName("acepta esa misma certificación en un turno de día que termina antes de que caduque")
    void acceptsACertificationThatCoversTheWholeDayShift() {
        var expiryDay = TODAY.plusDays(3);
        var shift = dayShift(expiryDay, 12);
        var operator = certifiedOperator(
                1L, certification(HAUL_TRUCK_TYPE_ID, TODAY.minusYears(1), expiryDay));
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(evaluation.isAccepted()).isTrue();
    }

    @Test
    @DisplayName("advierte, sin bloquear, cuando el turno es el que lleva la máquina a su umbral")
    void warnsWhenTheShiftReachesTheThreshold() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = availableEquipment(10L, "CAM-001", 242, 250);

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(evaluation.isAccepted()).isTrue();
        assertThat(codesOf(evaluation.warnings()))
                .containsExactly(AssignmentRuleCode.EQUIPMENT_WILL_REACH_THRESHOLD.name());
        assertThat(evaluation.warnings()).allMatch(violation -> violation.severity() == RuleSeverity.WARNING);
    }

    @Test
    @DisplayName("no repite la advertencia de umbral para una máquina que ya está bloqueada")
    void doesNotWarnAboutAMachineAlreadyBlocked() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = blockedEquipment(10L, "CAM-001");

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(evaluation.warnings()).isEmpty();
        assertThat(codesOf(evaluation.blockingViolations()))
                .containsExactly(AssignmentRuleCode.EQUIPMENT_NOT_AVAILABLE.name());
    }

    @Test
    @DisplayName("libera al operador y a la máquina en cuanto se suspende una asignación")
    void freesTheSlotWhenAnAssignmentIsCancelled() {
        var shift = dayShift(TODAY.plusDays(1), 12);
        var operator = certifiedOperator(1L, validCertification(HAUL_TRUCK_TYPE_ID));
        var equipment = availableEquipment(10L, "CAM-001", 100, 250);
        book(shift, operator, equipment);
        shift.getAssignments().getFirst().cancel("Reprogramación");

        var evaluation = evaluator.evaluate(new AssignmentContext(shift, operator, equipment));

        assertThat(evaluation.isAccepted()).isTrue();
    }
}
